package dev.jefersonsiqueira.clinicflow.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record ScheduleAppointmentRequest(
    @NotNull UUID patientId,
    @NotNull UUID doctorId,
    @NotNull UUID procedureId,
    @NotNull @Future Instant startsAt) {}
