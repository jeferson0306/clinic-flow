package dev.jefersonsiqueira.clinicflow.auth;

/**
 * Two roles, matching the two kinds of work this clinic actually has —
 * front-desk administration and clinical care — not a general-purpose
 * permission system built ahead of a role a third kind of user would need.
 */
public enum Role {
  /** Registers patients and doctors, maintains the procedure catalogue, books appointments. */
  ADMIN,
  /** Schedules and cancels their own clinical work: appointments, exam requests, exam results. */
  DOCTOR
}
