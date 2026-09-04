package dev.jefersonsiqueira.clinicflow.doctor;

import jakarta.validation.constraints.NotBlank;

/**
 * Presence and shape only, same split as {@code CreatePatientRequest}: CPF
 * and email format are brdoc's question, not Bean Validation's.
 */
public record CreateDoctorRequest(
    @NotBlank String fullName,
    @NotBlank String cpf,
    @NotBlank String email,
    @NotBlank String specialty,
    @NotBlank String licenseNumber) {}
