package dev.jefersonsiqueira.clinicflow.doctor;

import dev.jefersonsiqueira.clinicflow.common.DocumentMasking;
import java.time.Instant;
import java.util.UUID;

/** CPF masked, same reasoning as {@code PatientResponse}: public sandbox, real people. */
public record DoctorResponse(
    UUID id,
    String fullName,
    String maskedCpf,
    String email,
    String specialty,
    String licenseNumber,
    Instant createdAt) {

  public static DoctorResponse from(Doctor doctor) {
    return new DoctorResponse(
        doctor.id,
        doctor.fullName,
        DocumentMasking.maskCpf(doctor.cpf),
        doctor.email,
        doctor.specialty,
        doctor.licenseNumber,
        doctor.createdAt);
  }
}
