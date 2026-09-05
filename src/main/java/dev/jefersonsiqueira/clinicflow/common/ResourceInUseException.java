package dev.jefersonsiqueira.clinicflow.common;

/**
 * Thrown when a delete is rejected by a foreign-key constraint — a patient
 * with appointments or exam history, a doctor with either, a procedure with
 * appointments. Deliberately not caught and silently ignored: a clinic's
 * records referencing a deleted patient/doctor/procedure would be a data
 * integrity problem, not a convenience worth working around.
 */
public class ResourceInUseException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public ResourceInUseException(String message) {
    super(message);
  }
}
