-- Correction 2 (architectural review): street/city/state stop being
-- ViaCEP-only fields — the caller's own form always supplies them now
-- (pre-filled from ViaCEP as a courtesy, always editable, never blocked by
-- a slow or unlisted postcode). Every patient seeded before this migration
-- already has real values here (V7/V9/V10), so this backfill is a
-- defensive no-op for the existing dataset, not a fabrication — it only
-- catches a row that genuinely has a gap (e.g. the ViaCEP-down test
-- scenario, if it had ever reached production data).
update patients set street = 'Não informado' where street is null;
update patients set city = 'Não informado' where city is null;
update patients set state = 'NI' where state is null;

alter table patients
    alter column street set not null,
    alter column city set not null,
    alter column state set not null;
