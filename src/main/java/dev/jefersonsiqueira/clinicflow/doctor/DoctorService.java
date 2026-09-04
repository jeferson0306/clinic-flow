package dev.jefersonsiqueira.clinicflow.doctor;

import dev.jefersonsiqueira.clinicflow.common.ResourceInUseException;
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
public class DoctorService {

  private static final String FOREIGN_KEY_VIOLATION_SQLSTATE = "23503";

  @Inject DoctorRepository doctors;
  @Inject DocumentValidator documentValidator;

  @Transactional
  public Doctor register(CreateDoctorRequest request) {
    String cpf = documentValidator.cpf(request.cpf());
    if (doctors.existsByCpf(cpf)) {
      throw new DuplicateDoctorException("CPF");
    }

    String licenseNumber = request.licenseNumber().trim().toUpperCase();
    if (doctors.existsByLicenseNumber(licenseNumber)) {
      throw new DuplicateDoctorException("licence number");
    }

    String email = documentValidator.email(request.email());

    Doctor doctor = new Doctor();
    doctor.fullName = request.fullName().trim();
    doctor.cpf = cpf;
    doctor.email = email;
    doctor.specialty = request.specialty().trim();
    doctor.licenseNumber = licenseNumber;
    doctor.createdAt = Instant.now();

    doctors.persist(doctor);
    return doctor;
  }

  public Doctor findById(UUID id) {
    return doctors.findByIdOptional(id).orElseThrow(NoSuchElementException::new);
  }

  /** Every doctor, newest first — see PatientService.listAll's javadoc for why no pagination yet. */
  public List<Doctor> listAll() {
    return doctors.listAll(io.quarkus.panache.common.Sort.by("createdAt").descending());
  }

  @Transactional
  public Doctor update(UUID id, UpdateDoctorRequest request) {
    Doctor doctor = findById(id);
    String licenseNumber = request.licenseNumber().trim().toUpperCase();
    if (doctors.existsByLicenseNumberForAnotherDoctor(licenseNumber, id)) {
      throw new DuplicateDoctorException("licence number");
    }
    String email = documentValidator.email(request.email());

    doctor.fullName = request.fullName().trim();
    doctor.email = email;
    doctor.specialty = request.specialty().trim();
    doctor.licenseNumber = licenseNumber;
    return doctor;
  }

  @Transactional
  public void delete(UUID id) {
    Doctor doctor = findById(id);
    try {
      doctors.delete(doctor);
      doctors.getEntityManager().flush();
    } catch (ConstraintViolationException e) {
      if (FOREIGN_KEY_VIOLATION_SQLSTATE.equals(e.getSQLState())) {
        throw new ResourceInUseException(
            "This doctor has appointments or exams on record and cannot be deleted");
      }
      throw e;
    }
  }
}
