package dev.jefersonsiqueira.clinicflow.doctor;

import dev.jefersonsiqueira.clinicflow.validation.brdoc.DocumentValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

@ApplicationScoped
public class DoctorService {

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
}
