package dev.jefersonsiqueira.clinicflow.common;

/**
 * What kind of thing went wrong, independent of the HTTP status code that
 * happens to carry it. A 422 and a 409 are both {@code VALIDATION} or
 * {@code CONFLICT} depending on *why*, not on the number — this is the field
 * worth grepping logs for when triaging "is this a bug or a bad request".
 */
public enum ErrorCategory {
  /** The request is well-formed, but a value in it fails a rule this API or brdoc owns. */
  VALIDATION,
  /** The request is valid on its own, but conflicts with state that already exists. */
  CONFLICT,
  /** A referenced id — patient, doctor, procedure, appointment, exam — does not exist. */
  NOT_FOUND,
  /** Unexpected. Everything that is not one of the three above. */
  SYSTEM
}
