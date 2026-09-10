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
 * Written exclusively through {@link AuditLogService} — never construct or
 * persist this directly, or every caller would drift on how a row gets
 * built. Most rows come from {@link AuditLogFilter}, one generic entry per
 * successful HTTP call; a few — {@link AuditAction#CPF_CHANGED} is the
 * first — are written explicitly by a service that knows something a
 * generic HTTP filter cannot (old value, new value, the operator's stated
 * reason), which is what {@link #details} exists to hold.
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

  /** Free-form context a generic access log can't carry — e.g. the masked old/new CPF and stated reason for a {@link AuditAction#CPF_CHANGED} entry. Null for every ordinary access-log row. */
  @Column(name = "details")
  public String details;

  @Column(name = "occurred_at", nullable = false)
  public Instant occurredAt;
}
