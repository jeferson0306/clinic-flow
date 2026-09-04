package dev.jefersonsiqueira.clinicflow.appointment;

import dev.jefersonsiqueira.clinicflow.doctor.DoctorService;
import dev.jefersonsiqueira.clinicflow.patient.PatientService;
import dev.jefersonsiqueira.clinicflow.procedure.Procedure;
import dev.jefersonsiqueira.clinicflow.procedure.ProcedureService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;

@ApplicationScoped
public class AppointmentService {

  /**
   * Postgres's standard SQLSTATE for an exclusion-constraint violation.
   * {@code ConstraintViolationException.getConstraintName()} was tried first
   * and came back {@code null} here — Hibernate's name extractor is built
   * for {@code UNIQUE}/foreign-key violations and does not recognize the
   * message shape Postgres emits for {@code EXCLUDE}. The SQLSTATE is
   * standard and does not depend on Hibernate parsing anything.
   */
  private static final String EXCLUSION_VIOLATION_SQLSTATE = "23P01";

  @Inject AppointmentRepository appointments;
  @Inject PatientService patients;
  @Inject DoctorService doctors;
  @Inject ProcedureService procedures;

  @Transactional
  public Appointment schedule(ScheduleAppointmentRequest request) {
    // Existence, not availability: a doctor id that does not exist is a 404,
    // same as fetching it directly would report. Availability is what the
    // database's exclusion constraint below actually decides.
    patients.findById(request.patientId());
    doctors.findById(request.doctorId());
    Procedure procedure = procedures.findById(request.procedureId());

    Appointment appointment = new Appointment();
    appointment.patientId = request.patientId();
    appointment.doctorId = request.doctorId();
    appointment.procedureId = request.procedureId();
    appointment.startsAt = request.startsAt();
    appointment.endsAt = request.startsAt().plus(procedure.durationMinutes, ChronoUnit.MINUTES);
    appointment.status = Appointment.Status.SCHEDULED;
    appointment.createdAt = Instant.now();

    // persist() alone does not guarantee the INSERT — and so the exclusion
    // constraint check — happens before this method returns; Hibernate is
    // free to defer it to the transaction's own commit-time flush, by which
    // point this try/catch is long gone. The explicit flush is what makes the
    // constraint violation, if any, arrive here rather than as an opaque
    // failure at commit.
    try {
      appointments.persist(appointment);
      appointments.getEntityManager().flush();
    } catch (ConstraintViolationException e) {
      if (EXCLUSION_VIOLATION_SQLSTATE.equals(e.getSQLState())) {
        throw new DoubleBookingException();
      }
      throw e;
    }
    return appointment;
  }

  @Transactional
  public Appointment cancel(UUID id) {
    Appointment appointment = findById(id);
    // Cancelling, not deleting: the exclusion constraint only applies to
    // SCHEDULED rows (see V5), so this is what actually frees the slot for
    // someone else — a hard delete would too, but would also erase the
    // record that an appointment was ever booked and cancelled.
    appointment.status = Appointment.Status.CANCELLED;
    return appointment;
  }

  public Appointment findById(UUID id) {
    return appointments.findByIdOptional(id).orElseThrow(NoSuchElementException::new);
  }

  /** Every appointment, newest first — see PatientService.listAll's javadoc for why no pagination yet. */
  public List<Appointment> listAll() {
    return appointments.listAll(io.quarkus.panache.common.Sort.by("createdAt").descending());
  }
}
