package dev.jefersonsiqueira.clinicflow.patient;

import dev.jefersonsiqueira.clinicflow.common.validation.NamePattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * Presence and shape only — {@code @NotBlank}, {@code @Past}, {@code
 * fullName}'s {@link NamePattern}. Whether a CPF, email or postcode is
 * actually *valid* is brdoc's question, not Bean Validation's: brdoc already
 * gives a better answer than a regex would, so this record does not compete
 * with it for those three.
 *
 * {@code phone} and {@code birthDate} are required, not optional: a clinic
 * cannot reschedule an appointment or deliver an exam result without a
 * working phone number, and {@code birthDate} being present is what lets
 * {@link RequiresGuardianIfMinor} actually run its guardian check at all —
 * an absent one used to mean "age unknown, skip the check," which was a
 * real way to register a minor with no guardian on file. Everything from
 * {@code socialName} down stays genuinely optional clinical/legal-guardian
 * data, filled in progressively over a patient's actual visits rather than
 * all at intake.
 *
 * {@code street}, {@code city} and {@code state} are required inputs too,
 * not fields {@link dev.jefersonsiqueira.clinicflow.address.AddressLookupService}
 * derives on its own anymore: ViaCEP is a courtesy that pre-fills them in
 * the caller's own form, but the caller always sends the final value, typed
 * or accepted from the lookup. That is what makes a real address possible
 * even when the postcode is new, rural, or ViaCEP is simply down — the
 * previous design left the whole address null in exactly that case.
 * {@code district} stays optional; not every postcode has a formal bairro.
 */
@RequiresGuardianIfMinor
public record CreatePatientRequest(
    @NotBlank @Pattern(regexp = NamePattern.REGEXP, message = NamePattern.MESSAGE) String fullName,
    @NotBlank String cpf,
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
