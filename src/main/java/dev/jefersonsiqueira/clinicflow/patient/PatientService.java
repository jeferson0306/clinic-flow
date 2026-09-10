package dev.jefersonsiqueira.clinicflow.patient;

import dev.jefersonsiqueira.clinicflow.address.Address;
import dev.jefersonsiqueira.clinicflow.address.AddressLookupService;
import dev.jefersonsiqueira.clinicflow.audit.AuditAction;
import dev.jefersonsiqueira.clinicflow.audit.AuditLogService;
import dev.jefersonsiqueira.clinicflow.common.DocumentMasking;
import dev.jefersonsiqueira.clinicflow.common.ResourceInUseException;
import dev.jefersonsiqueira.clinicflow.validation.brdoc.DocumentValidationException;
import dev.jefersonsiqueira.clinicflow.validation.brdoc.DocumentValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;

@ApplicationScoped
public class PatientService {

  /** Postgres's standard SQLSTATE for a foreign-key violation. */
  private static final String FOREIGN_KEY_VIOLATION_SQLSTATE = "23503";

  @Inject PatientRepository patients;
  @Inject DocumentValidator documentValidator;
  @Inject AddressLookupService addressLookup;
  @Inject AuditLogService auditLog;

  @Transactional
  public Patient register(CreatePatientRequest request) {
    // CPF is validated and normalized before the duplicate check: two typings
    // of the same CPF — with or without punctuation — must collide, not slip
    // past as two different strings.
    String cpf = documentValidator.cpf(request.cpf());
    if (patients.existsByCpf(cpf)) {
      throw new DuplicatePatientException();
    }

    String email = documentValidator.email(request.email());
    String phone = documentValidator.telephone(request.phone());
    Address address = addressLookup.resolve(
        request.postcode(), request.street().trim(), blankToNull(request.district()), request.city().trim(),
        request.state().trim());
    address.houseNumber = request.houseNumber().trim();
    address.complement = blankToNull(request.complement());

    Patient patient = new Patient();
    patient.fullName = request.fullName().trim();
    patient.cpf = cpf;
    patient.email = email;
    patient.phone = phone;
    patient.birthDate = request.birthDate();
    patient.address = address;
    patient.createdAt = Instant.now();
    applyClinicalFields(patient, request.socialName(), request.motherName(), request.sex(), request.bloodType(),
        request.allergies(), request.continuousMedications(), request.preExistingConditions(),
        request.clinicalAlert(), request.guardianName(), request.guardianCpf(), request.guardianRelationship(),
        request.guardianPhone());

    patients.persist(patient);
    return patient;
  }

  /**
   * Shared by register and update — the two request records carry the same
   * clinical/guardian fields, and this is the one place that trims blanks to
   * null and normalizes the guardian CPF the same way the patient's own is.
   */
  private void applyClinicalFields(
      Patient patient,
      String socialName,
      String motherName,
      Sex sex,
      BloodType bloodType,
      String allergies,
      String continuousMedications,
      String preExistingConditions,
      String clinicalAlert,
      String guardianName,
      String guardianCpf,
      GuardianRelationship guardianRelationship,
      String guardianPhone) {
    patient.socialName = blankToNull(socialName);
    patient.motherName = blankToNull(motherName);
    patient.sex = sex;
    patient.bloodType = bloodType;
    patient.allergies = blankToNull(allergies);
    patient.continuousMedications = blankToNull(continuousMedications);
    patient.preExistingConditions = blankToNull(preExistingConditions);
    patient.clinicalAlert = blankToNull(clinicalAlert);
    patient.guardianName = blankToNull(guardianName);
    patient.guardianCpf =
        guardianCpf == null || guardianCpf.isBlank() ? null : documentValidator.cpf(guardianCpf, "guardianCpf");
    patient.guardianRelationship = guardianRelationship;
    patient.guardianPhone = blankToNull(guardianPhone);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public Patient findById(UUID id) {
    return patients.findByIdOptional(id).orElseThrow(NoSuchElementException::new);
  }

  /**
   * Every patient, newest first. No pagination: this is a demo dataset,
   * measured in tens of rows, not the kind of table a real clinic's history
   * would eventually become — that is a real constraint to add when the
   * dataset it is sized for actually exists, not before.
   */
  public List<Patient> listAll() {
    return patients.listAll(io.quarkus.panache.common.Sort.by("createdAt").descending());
  }

  /**
   * {@code actorEmail}/{@code actorRole}/{@code ipAddress} exist purely for
   * the CPF-change audit entry below — see {@link
   * dev.jefersonsiqueira.clinicflow.audit.AuditLogService#record(String,
   * String, AuditAction, String, UUID, String, String)}'s own javadoc for
   * why that write has to share this method's transaction rather than
   * happen later in a response filter the way every other access-log entry
   * does.
   */
  @Transactional
  public Patient update(
      UUID id, UpdatePatientRequest request, String actorEmail, String actorRole, String ipAddress) {
    Patient patient = findById(id);

    // cpf is optional here the same way guardianCpf is: PatientResponse only
    // ever returns a masked value, so a client resubmitting an untouched
    // form has nothing real to send back. Blank means "keep the one on
    // file"; a real, different value is a genuine correction, which needs a
    // stated reason and leaves a trail — see RequiresGuardianIfMinor's
    // sibling reasoning on UpdatePatientRequest for the same pattern.
    if (request.cpf() != null && !request.cpf().isBlank()) {
      String newCpf = documentValidator.cpf(request.cpf());
      if (!newCpf.equals(patient.cpf)) {
        if (request.cpfChangeReason() == null || request.cpfChangeReason().isBlank()) {
          throw new DocumentValidationException(
              "cpfChangeReason", "A reason is required when changing a patient's CPF");
        }
        if (patients.existsByCpfForAnotherPatient(newCpf, id)) {
          throw new DuplicatePatientException();
        }
        auditLog.record(
            actorEmail,
            actorRole,
            AuditAction.CPF_CHANGED,
            "PATIENT",
            id,
            ipAddress,
            "cpf %s -> %s; reason: %s"
                .formatted(
                    DocumentMasking.maskCpf(patient.cpf),
                    DocumentMasking.maskCpf(newCpf),
                    request.cpfChangeReason().trim()));
        patient.cpf = newCpf;
      }
    }

    String email = documentValidator.email(request.email());
    String phone = documentValidator.telephone(request.phone());
    Address address = addressLookup.resolve(
        request.postcode(), request.street().trim(), blankToNull(request.district()), request.city().trim(),
        request.state().trim());
    address.houseNumber = request.houseNumber().trim();
    address.complement = blankToNull(request.complement());

    // guardianCpf comes back masked in every response (PatientResponse), so
    // a client resubmitting a form it never touched can't round-trip it
    // unchanged — a blank guardianCpf here means "leave it as it was," not
    // "clear it," unlike every other field on this request.
    String existingGuardianCpf = patient.guardianCpf;

    patient.fullName = request.fullName().trim();
    patient.email = email;
    patient.phone = phone;
    patient.birthDate = request.birthDate();
    patient.address = address;
    applyClinicalFields(patient, request.socialName(), request.motherName(), request.sex(), request.bloodType(),
        request.allergies(), request.continuousMedications(), request.preExistingConditions(),
        request.clinicalAlert(), request.guardianName(), request.guardianCpf(), request.guardianRelationship(),
        request.guardianPhone());
    if (request.guardianCpf() == null || request.guardianCpf().isBlank()) {
      patient.guardianCpf = existingGuardianCpf;
    }
    return patient;
  }

  @Transactional
  public void delete(UUID id) {
    Patient patient = findById(id);
    try {
      patients.delete(patient);
      patients.getEntityManager().flush();
    } catch (ConstraintViolationException e) {
      if (FOREIGN_KEY_VIOLATION_SQLSTATE.equals(e.getSQLState())) {
        throw new ResourceInUseException(
            "This patient has appointments or exams on record and cannot be deleted");
      }
      throw e;
    }
  }
}
