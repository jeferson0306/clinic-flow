package dev.jefersonsiqueira.clinicflow.doctor;

/** A doctor with this CPF or licence number is already registered. */
public class DuplicateDoctorException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public DuplicateDoctorException(String field) {
    super("A doctor with this " + field + " is already registered");
    this.field = field;
  }

  private final String field;

  public String field() {
    return field;
  }
}
