package dev.jefersonsiqueira.clinicflow.audit;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/**
 * One row per access to a PHI-bearing resource — who, what action, on
 * which record, from where, when. Insert-only: nothing in this codebase
 * ever updates or deletes a row here, by design (see V13's own comment).
 * Written exclusively by {@link AuditLogFilter} — never construct or
 * persist this from a resource/service directly, or the two would drift.
 */
@Entity
@Table(name = "audit_log")
public class AuditLogEntry extends PanacheEntityBase {

  @Id @GeneratedValue @UuidGenerator public UUID id;

  @Column(name = "actor_email")
  public String actorEmail;

  @Column(name = "actor_role")
  public String actorRole;

  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false)
  public AuditAction action;

  @Column(name = "resource_type", nullable = false)
  public String resourceType;

  @Column(name = "resource_id")
  public UUID resourceId;

  @Column(name = "ip_address")
  public String ipAddress;

  @Column(name = "occurred_at", nullable = false)
  public Instant occurredAt;
}
