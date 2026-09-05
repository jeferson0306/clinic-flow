package dev.jefersonsiqueira.clinicflow.appointment;

/** This doctor already has a scheduled appointment overlapping the requested time. */
public class DoubleBookingException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public DoubleBookingException() {
    super("This doctor already has an appointment scheduled in that time range");
  }
}
