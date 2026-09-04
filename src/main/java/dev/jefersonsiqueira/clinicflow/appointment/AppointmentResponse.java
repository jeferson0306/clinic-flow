package dev.jefersonsiqueira.clinicflow.appointment;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
    UUID id,
    UUID patientId,
    UUID doctorId,
    UUID procedureId,
    Instant startsAt,
    Instant endsAt,
    Appointment.Status status) {

  public static AppointmentResponse from(Appointment appointment) {
    return new AppointmentResponse(
        appointment.id,
        appointment.patientId,
        appointment.doctorId,
        appointment.procedureId,
        appointment.startsAt,
        appointment.endsAt,
        appointment.status);
  }
}
