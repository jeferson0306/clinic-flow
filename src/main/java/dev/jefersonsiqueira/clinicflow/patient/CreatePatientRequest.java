package dev.jefersonsiqueira.clinicflow.patient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;

/**
 * Presence and shape only — {@code @NotBlank}, {@code @Past}. Whether a CPF,
 * email or postcode is actually *valid* is brdoc's question, not Bean
 * Validation's: brdoc already gives a better answer than a regex would, so
 * this record does not compete with it.
 */
public record CreatePatientRequest(
    @NotBlank String fullName,
    @NotBlank String cpf,
    @NotBlank String email,
    String phone,
    @Past LocalDate birthDate,
    @NotBlank String postcode) {}
