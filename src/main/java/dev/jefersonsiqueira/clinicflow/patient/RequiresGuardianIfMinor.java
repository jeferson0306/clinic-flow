package dev.jefersonsiqueira.clinicflow.patient;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.Period;

/**
 * A class-level constraint, not a field-level one: whether a guardian is
 * required depends on two fields together ({@code birthDate} and the
 * guardian ones), which no single-field annotation can express. Applied to
 * {@link CreatePatientRequest} and {@link UpdatePatientRequest} — both
 * records implement {@link Subject} for free, since a record's accessors
 * already match the interface's method names.
 *
 * A null {@code birthDate} is not treated as "definitely a minor" — age is
 * simply unknown in that case, the same lenient stance the rest of this
 * record already takes on an optional field.
 *
 * {@code requireCpf}: {@link CreatePatientRequest} needs all three guardian
 * fields, but {@link UpdatePatientRequest} does not — {@code PatientResponse}
 * only ever returns the guardian's CPF masked, so a client resubmitting an
 * edit form it never touched has no valid value to send back for it.
 * {@link dev.jefersonsiqueira.clinicflow.patient.PatientService#update}
 * treats a blank {@code guardianCpf} there as "keep the one on file," which
 * this constraint has to allow through rather than reject at the request
 * layer before that logic ever runs.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequiresGuardianIfMinor.Validator.class)
public @interface RequiresGuardianIfMinor {

  String message() default "a legal guardian is required for a patient under 18";

  boolean requireCpf() default true;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  interface Subject {
    LocalDate birthDate();

    String guardianName();

    String guardianCpf();

    // Object, not GuardianRelationship: this interface is shape-only and
    // doesn't need to know the enum type, it only ever checks for null.
    Object guardianRelationship();

    // Required for a minor same as the other three: a guardian this system
    // can't reach by phone isn't meaningfully "on file" for anything the
    // phone would actually be needed for — rescheduling, an exam result,
    // an emergency.
    String guardianPhone();
  }

  class Validator implements ConstraintValidator<RequiresGuardianIfMinor, Subject> {

    private static final int AGE_OF_MAJORITY = 18;

    private boolean requireCpf;

    @Override
    public void initialize(RequiresGuardianIfMinor annotation) {
      this.requireCpf = annotation.requireCpf();
    }

    @Override
    public boolean isValid(Subject subject, ConstraintValidatorContext context) {
      if (subject.birthDate() == null) {
        return true;
      }
      int age = Period.between(subject.birthDate(), LocalDate.now()).getYears();
      if (age >= AGE_OF_MAJORITY) {
        return true;
      }
      boolean cpfOk = !requireCpf || hasText(subject.guardianCpf());
      if (hasText(subject.guardianName())
          && cpfOk
          && subject.guardianRelationship() != null
          && hasText(subject.guardianPhone())) {
        return true;
      }

      // Reported on guardianName specifically — GlobalExceptionMapper's
      // ConstraintViolationException branch keys fieldErrors on the last
      // path segment, and a single field is a clearer place to surface this
      // than the record type itself.
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              "guardian name, CPF, relationship and phone are required for a patient under 18")
          .addPropertyNode("guardianName")
          .addConstraintViolation();
      return false;
    }

    private static boolean hasText(String value) {
      return value != null && !value.isBlank();
    }
  }
}
