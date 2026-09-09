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
    AuditLogEntry entry = new AuditLogEntry();
    entry.actorEmail = actorEmail;
    entry.actorRole = actorRole;
    entry.action = action;
    entry.resourceType = resourceType;
    entry.resourceId = resourceId;
    entry.ipAddress = ipAddress;
    entry.occurredAt = Instant.now();
    repository.persist(entry);
  }
}
