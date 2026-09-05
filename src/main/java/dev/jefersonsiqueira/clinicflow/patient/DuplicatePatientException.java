package dev.jefersonsiqueira.clinicflow.patient;

/** A patient with this CPF is already registered. */
public class DuplicatePatientException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public DuplicatePatientException() {
    super("A patient with this CPF is already registered");
  }
}
