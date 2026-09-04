package dev.jefersonsiqueira.clinicflow.doctor;

import jakarta.validation.constraints.NotBlank;

/** No {@code cpf} — same reasoning as {@link dev.jefersonsiqueira.clinicflow.patient.UpdatePatientRequest}. */
public record UpdateDoctorRequest(
    @NotBlank String fullName,
    @NotBlank String email,
    @NotBlank String specialty,
    @NotBlank String licenseNumber) {}
