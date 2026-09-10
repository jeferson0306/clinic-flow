package dev.jefersonsiqueira.clinicflow.audit;

public enum AuditAction {
  VIEWED,
  CREATED,
  UPDATED,
  DELETED,
  /** A patient's own CPF was changed on an existing record — see PatientService#update. */
  CPF_CHANGED
}
