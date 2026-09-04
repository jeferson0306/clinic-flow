package dev.jefersonsiqueira.clinicflow.calendar;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

/**
 * The free-slot math, isolated from where its inputs come from — a working
 * window and a list of what is already booked go in, a list of open slots
 * comes out. Nothing here touches config, the database or CDI, which is what
 * makes it a plain, fast unit test rather than one that needs a Quarkus
 * context or a Testcontainers Postgres to exercise.
 */
public final class AvailabilityCalculator {

  private AvailabilityCalculator() {}

  /**
   * Every {@code durationMinutes}-sized slot between {@code windowStart} and
   * {@code windowEnd} that does not overlap anything in {@code booked}, in
   * order.
   */
  public static List<TimeSlot> freeSlots(
      Instant windowStart, Instant windowEnd, int durationMinutes, List<TimeSlot> booked) {
    return candidateStarts(windowStart, windowEnd, durationMinutes)
        .map(start -> new TimeSlot(start, start.plus(durationMinutes, ChronoUnit.MINUTES)))
        .filter(candidate -> booked.stream().noneMatch(candidate::overlaps))
        .toList();
  }

  private static Stream<Instant> candidateStarts(Instant windowStart, Instant windowEnd, int durationMinutes) {
    return Stream.iterate(windowStart, start -> start.plus(durationMinutes, ChronoUnit.MINUTES))
        .takeWhile(start -> !start.plus(durationMinutes, ChronoUnit.MINUTES).isAfter(windowEnd));
  }
}
