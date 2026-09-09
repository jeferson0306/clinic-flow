package dev.jefersonsiqueira.clinicflow.audit;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/**
 * A repository, not the active-record {@code PanacheEntity} statics — same
 * reasoning as {@code PatientRepository}: callers depend on this injectable
 * interface, not on a static method on {@link AuditLogEntry}.
 */
@ApplicationScoped
public class AuditLogRepository implements PanacheRepositoryBase<AuditLogEntry, UUID> {}
