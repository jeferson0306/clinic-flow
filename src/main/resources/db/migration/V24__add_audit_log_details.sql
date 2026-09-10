-- Correction 3: a CPF change needs to carry more than "who did what to
-- which resource" — the masked old/new value and the operator's stated
-- reason. `details` is nullable and stays null for every ordinary
-- access-log row the AuditLogFilter already writes; only the new
-- CPF_CHANGED action (and any future one that needs it) populates it.
alter table audit_log add column details text;
