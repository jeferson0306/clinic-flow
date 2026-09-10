package dev.jefersonsiqueira.clinicflow.audit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class AuditLogService {

  @Inject AuditLogRepository repository;

  @Transactional
  public void record(
      String actorEmail, String actorRole, AuditAction action, String resourceType, UUID resourceId, String ipAddress) {
    record(actorEmail, actorRole, action, resourceType, resourceId, ipAddress, null);
  }

  /**
   * Same as the five-arg overload, plus {@code details} — used by callers
   * (like {@code PatientService#update}'s CPF-change path) that know
   * something worth recording beyond "this action happened on this
   * resource." Runs in the caller's own transaction rather than opening a
   * new one, so a CPF change and its audit entry commit or roll back
   * together — an audit trail for changing a patient's identity is not
   * something this app treats as best-effort.
   */
  public void record(
      String actorEmail,
      String actorRole,
      AuditAction action,
      String resourceType,
      UUID resourceId,
      String ipAddress,
      String details) {
    AuditLogEntry entry = new AuditLogEntry();
    entry.actorEmail = actorEmail;
    entry.actorRole = actorRole;
    entry.action = action;
    entry.resourceType = resourceType;
    entry.resourceId = resourceId;
    entry.ipAddress = ipAddress;
    entry.details = details;
    entry.occurredAt = Instant.now();
    repository.persist(entry);
  }
}
