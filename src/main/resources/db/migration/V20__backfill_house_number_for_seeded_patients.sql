-- V17 added house_number/complement to patients as nullable columns with
-- no backfill, "by design" the same way V12 left ibge_code nullable — fine
-- at the time, but house_number then became a *required* field on the
-- frontend's create/edit forms (see clinic-flow-web's patient dialogs),
-- so every one of the patients seeded before V17 (V7/V9/V10/V15, 51 rows)
-- has been returning house_number: null ever since — a value the new
-- frontend contract never expected to see, and the exact "address missing
-- a piece it should have" symptom reported live.
--
-- A deterministic, varied number per row (not a single repeated value,
-- which would look obviously fake) rather than a real building number
-- nobody actually verified — same spirit as this seed data's already-fake
-- CPFs: plausible, not authoritative.
with numbered as (
  select id, row_number() over (order by created_at, id) as rn
  from patients
  where house_number is null
)
update patients
set house_number = (100 + (numbered.rn * 7) % 900)::text
from numbered
where patients.id = numbered.id;
