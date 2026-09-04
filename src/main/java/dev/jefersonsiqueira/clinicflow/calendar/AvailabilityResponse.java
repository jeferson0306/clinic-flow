package dev.jefersonsiqueira.clinicflow.calendar;

import java.util.List;
import java.util.UUID;

public record AvailabilityResponse(UUID doctorId, UUID procedureId, List<TimeSlot> freeSlots) {}
