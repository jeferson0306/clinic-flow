package dev.jefersonsiqueira.clinicflow.patient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;

/**
 * No {@code cpf} field, deliberately: a patient's CPF is their identity in
 * this system (the uniqueness key {@link PatientService#register} checks
 * against) — changing it would silently detach a record from the person it
 * was created for. A genuine CPF correction is a delete-and-recreate, not an
 * update, the same way {@link DoctorService} and {@link
 * dev.jefersonsiqueira.clinicflow.procedure.ProcedureService} treat theirs.
 */
public record UpdatePatientRequest(
    @NotBlank String fullName,
    @NotBlank String email,
    String phone,
    @Past LocalDate birthDate,
    @NotBlank String postcode) {}
