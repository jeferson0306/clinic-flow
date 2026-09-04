package dev.jefersonsiqueira.clinicflow.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A plain unit test — no {@code @QuarkusTest}, no Testcontainers, no
 * database. {@link AvailabilityCalculator} takes ordinary values in and
 * returns ordinary values out, which is what makes this fast enough to run
 * on every keystroke rather than only in CI.
 */
class AvailabilityCalculatorTest {

  private static final Instant DAY_START = Instant.parse("2026-09-10T11:00:00Z"); // 08:00 in UTC-3
  private static final Instant DAY_END = Instant.parse("2026-09-10T21:00:00Z"); // 18:00 in UTC-3

  @Test
  void fillsTheWholeWindowWhenNothingIsBooked() {
    List<TimeSlot> slots = AvailabilityCalculator.freeSlots(DAY_START, DAY_END, 30, List.of());

    assertThat(slots).hasSize(20); // ten hours, thirty-minute slots
    assertThat(slots.getFirst().startsAt()).isEqualTo(DAY_START);
    assertThat(slots.getLast().endsAt()).isEqualTo(DAY_END);
  }

  @Test
  void removesOnlyTheSlotsThatOverlapABooking() {
    // Booked 09:00-09:45 (UTC-3) — spans the 09:00 and 09:30 candidate starts.
    TimeSlot booked = new TimeSlot(
        DAY_START.plus(1, ChronoUnit.HOURS), DAY_START.plus(1, ChronoUnit.HOURS).plus(45, ChronoUnit.MINUTES));

    List<TimeSlot> slots = AvailabilityCalculator.freeSlots(DAY_START, DAY_END, 30, List.of(booked));

    assertThat(slots).hasSize(18).noneMatch(slot -> slot.overlaps(booked));
  }

  @Test
  void aSlotThatMerelyTouchesABookingsBoundaryIsNotAnOverlap() {
    // Booked exactly the first thirty minutes — back-to-back, not overlapping.
    TimeSlot booked = new TimeSlot(DAY_START, DAY_START.plus(30, ChronoUnit.MINUTES));

    List<TimeSlot> slots = AvailabilityCalculator.freeSlots(DAY_START, DAY_END, 30, List.of(booked));

    assertThat(slots).hasSize(19).first().extracting(TimeSlot::startsAt).isEqualTo(booked.endsAt());
  }

  @Test
  void aWindowShorterThanOneSlotHasNoAvailability() {
    List<TimeSlot> slots =
        AvailabilityCalculator.freeSlots(DAY_START, DAY_START.plus(10, ChronoUnit.MINUTES), 30, List.of());

    assertThat(slots).isEmpty();
  }
}
