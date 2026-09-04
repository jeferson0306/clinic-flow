package dev.jefersonsiqueira.clinicflow.patient;

/** A patient with this CPF is already registered. */
public class DuplicatePatientException extends RuntimeException {
  public DuplicatePatientException() {
    super("A patient with this CPF is already registered");
  }
}
