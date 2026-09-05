package dev.jefersonsiqueira.clinicflow.doctor;

import dev.jefersonsiqueira.clinicflow.common.validation.NamePattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** No {@code cpf} — same reasoning as {@link dev.jefersonsiqueira.clinicflow.patient.UpdatePatientRequest}. */
public record UpdateDoctorRequest(
    @NotBlank @Pattern(regexp = NamePattern.REGEXP, message = NamePattern.MESSAGE) String fullName,
    @NotBlank String email,
    @NotBlank String specialty,
    @NotBlank String licenseNumber) {}
