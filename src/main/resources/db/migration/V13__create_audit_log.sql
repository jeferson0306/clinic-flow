-- Insert-only: nothing in this codebase ever updates or deletes a row here.
-- Scoped to the two resources that carry real PHI (patients, exams) — see
-- AuditLogFilter for why the other resources aren't logged.
create table audit_log (
    id                uuid primary key,
    actor_email       text,
    actor_role        text,
    action            text not null,
    resource_type     text not null,
    resource_id       uuid,
    ip_address        text,
    occurred_at       timestamptz not null
);

create index audit_log_resource_idx on audit_log (resource_type, resource_id);
create index audit_log_occurred_at_idx on audit_log (occurred_at);
