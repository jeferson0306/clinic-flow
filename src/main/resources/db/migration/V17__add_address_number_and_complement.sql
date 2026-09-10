-- Neither is derivable from the postcode (ViaCEP resolves a street, not
-- which building on it) — both come from the caller. Nullable at the schema
-- level for the same reason every other column added after V1 is: existing
-- rows stay valid with no backfill, and house_number's requiredness is
-- enforced at the request layer (CreatePatientRequest), not here.
alter table patients
    add column house_number text,
    add column complement   text;
