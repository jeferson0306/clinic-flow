package dev.jefersonsiqueira.clinicflow.appointment;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AppointmentRepository implements PanacheRepositoryBase<Appointment, UUID> {

  /** Every SCHEDULED appointment for this doctor that overlaps {@code [from, to)}. */
  public List<Appointment> scheduledOverlapping(UUID doctorId, Instant from, Instant to) {
    return list(
        "doctorId = ?1 and status = ?2 and startsAt < ?3 and endsAt > ?4",
        doctorId,
        Appointment.Status.SCHEDULED,
        to,
        from);
  }
}
