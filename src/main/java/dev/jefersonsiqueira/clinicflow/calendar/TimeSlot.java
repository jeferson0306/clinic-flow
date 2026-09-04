package dev.jefersonsiqueira.clinicflow.calendar;

import java.time.Instant;

/** A half-open interval, {@code [startsAt, endsAt)} — the same shape an appointment books. */
public record TimeSlot(Instant startsAt, Instant endsAt) {

  public boolean overlaps(TimeSlot other) {
    return startsAt.isBefore(other.endsAt) && other.startsAt.isBefore(endsAt);
  }
}
