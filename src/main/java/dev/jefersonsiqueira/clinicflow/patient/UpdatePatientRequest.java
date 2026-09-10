package dev.jefersonsiqueira.clinicflow.patient;

import dev.jefersonsiqueira.clinicflow.common.validation.NamePattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * {@code cpf} is optional here on purpose, the same shape as {@code
 * guardianCpf} just below it: {@link PatientResponse} only ever returns a
 * masked CPF, so a client resubmitting an edit form it never touched has no
 * real value to round-trip. A blank {@code cpf} means "keep the one on
 * file"; a non-blank one is a genuine correction, which {@link
 * PatientService#update} accepts — with a required {@code cpfChangeReason}
 * and a mandatory audit entry — rather than forcing a delete-and-recreate
 * that would orphan every appointment and exam pointing at this patient's
 * id. {@link DoctorService} and {@link
 * dev.jefersonsiqueira.clinicflow.procedure.ProcedureService} still forbid
 * changing their own identity fields; a patient's CPF is different because
 * check-in typos are the actual, common failure mode this exists to fix.
 */
@RequiresGuardianIfMinor(requireCpf = false)
public record UpdatePatientRequest(
    @NotBlank @Pattern(regexp = NamePattern.REGEXP, message = NamePattern.MESSAGE) String fullName,
    String cpf,
    // Required only when cpf actually differs from the one on file — a
    // conditional rule Bean Validation can't express on its own, so
    // PatientService enforces it once it knows whether this is a real
    // change.
    String cpfChangeReason,
    @NotBlank String email,
    @NotBlank String phone,
    @NotNull @Past LocalDate birthDate,
    @NotBlank String postcode,
    @NotBlank String houseNumber,
    String complement,
    @NotBlank String street,
    String district,
    @NotBlank String city,
    @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "must be a 2-letter state code") String state,
    String socialName,
    String motherName,
    Sex sex,
    BloodType bloodType,
    String allergies,
    String continuousMedications,
    String preExistingConditions,
    String clinicalAlert,
    String guardianName,
    String guardianCpf,
    GuardianRelationship guardianRelationship,
    String guardianPhone)
    implements RequiresGuardianIfMinor.Subject {}
