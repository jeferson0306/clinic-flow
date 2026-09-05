package dev.jefersonsiqueira.clinicflow.doctor;

import dev.jefersonsiqueira.clinicflow.common.validation.NamePattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Presence and shape only, same split as {@code CreatePatientRequest}: CPF
 * and email format are brdoc's question, not Bean Validation's.
 */
public record CreateDoctorRequest(
    @NotBlank @Pattern(regexp = NamePattern.REGEXP, message = NamePattern.MESSAGE) String fullName,
    @NotBlank String cpf,
    @NotBlank String email,
    @NotBlank String specialty,
    @NotBlank String licenseNumber) {}
