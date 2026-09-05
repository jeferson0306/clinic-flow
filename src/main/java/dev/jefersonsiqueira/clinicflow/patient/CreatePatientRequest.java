package dev.jefersonsiqueira.clinicflow.patient;

import dev.jefersonsiqueira.clinicflow.common.validation.NamePattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * Presence and shape only — {@code @NotBlank}, {@code @Past}, {@code
 * fullName}'s {@link NamePattern}. Whether a CPF, email or postcode is
 * actually *valid* is brdoc's question, not Bean Validation's: brdoc already
 * gives a better answer than a regex would, so this record does not compete
 * with it for those three.
 */
public record CreatePatientRequest(
    @NotBlank @Pattern(regexp = NamePattern.REGEXP, message = NamePattern.MESSAGE) String fullName,
    @NotBlank String cpf,
    @NotBlank String email,
    String phone,
    @Past LocalDate birthDate,
    @NotBlank String postcode) {}
