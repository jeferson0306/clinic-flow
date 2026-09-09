package dev.jefersonsiqueira.clinicflow.auth;

/**
 * Four roles, matching the four kinds of user this clinic actually has —
 * not a general-purpose permission system built ahead of a role a fifth
 * kind of user would need.
 */
public enum Role {
  /** Registers patients and doctors, maintains the procedure catalogue, books appointments. */
  ADMIN,
  /** Schedules and cancels their own clinical work: appointments, exam requests, exam results. */
  DOCTOR,
  /** Front desk: registers and updates patients, books and cancels appointments — not doctors, procedures or exams. */
  RECEPCAO,
  /** A single patient, scoped to their own record only — see MeResource, never a plain PatientResource route. */
  PACIENTE
}
