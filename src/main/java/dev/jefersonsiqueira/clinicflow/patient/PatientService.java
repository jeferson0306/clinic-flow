package dev.jefersonsiqueira.clinicflow.patient;

import dev.jefersonsiqueira.clinicflow.address.Address;
import dev.jefersonsiqueira.clinicflow.address.AddressLookupService;
import dev.jefersonsiqueira.clinicflow.validation.brdoc.DocumentValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@ApplicationScoped
public class PatientService {

  @Inject PatientRepository patients;
  @Inject DocumentValidator documentValidator;
  @Inject AddressLookupService addressLookup;

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
    String phone = request.phone() == null || request.phone().isBlank()
        ? null
        : documentValidator.telephone(request.phone());
    Address address = addressLookup.resolve(request.postcode());

    Patient patient = new Patient();
    patient.fullName = request.fullName().trim();
    patient.cpf = cpf;
    patient.email = email;
    patient.phone = phone;
    patient.birthDate = request.birthDate();
    patient.address = address;
    patient.createdAt = Instant.now();

    patients.persist(patient);
    return patient;
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
}
