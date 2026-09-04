package dev.jefersonsiqueira.clinicflow.calendar;

import dev.jefersonsiqueira.clinicflow.appointment.AppointmentRepository;
import dev.jefersonsiqueira.clinicflow.doctor.DoctorService;
import dev.jefersonsiqueira.clinicflow.procedure.Procedure;
import dev.jefersonsiqueira.clinicflow.procedure.ProcedureService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resolves the day's working window and what is already booked, then hands
 * both to {@link AvailabilityCalculator} — the boundary between "where this
 * information comes from" and the free-slot math itself.
 */
@ApplicationScoped
public class AvailabilityService {

  @Inject WorkingHoursConfig workingHours;
  @Inject AppointmentRepository appointments;
  @Inject DoctorService doctors;
  @Inject ProcedureService procedures;

  public List<TimeSlot> freeSlotsFor(UUID doctorId, LocalDate date, UUID procedureId) {
    // Existence, same reasoning as every other module: an unknown doctor or
    // procedure id is a 404, not an empty availability list pretending they
    // were simply booked solid.
    doctors.findById(doctorId);
    Procedure procedure = procedures.findById(procedureId);

    var windowStart = ZonedDateTime.of(date, workingHours.start(), workingHours.zoneId()).toInstant();
    var windowEnd = ZonedDateTime.of(date, workingHours.end(), workingHours.zoneId()).toInstant();

    List<TimeSlot> booked =
        appointments.scheduledOverlapping(doctorId, windowStart, windowEnd).stream()
            .map(appointment -> new TimeSlot(appointment.startsAt, appointment.endsAt))
            .toList();

    return AvailabilityCalculator.freeSlots(windowStart, windowEnd, procedure.durationMinutes, booked);
  }
}
