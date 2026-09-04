-- Needed for an exclusion constraint over a plain equality column (doctor_id)
-- combined with a range overlap (&&) in the same GiST index; without it,
-- GiST has no operator class for "=" on a uuid.
create extension if not exists btree_gist;

create table appointments (
    id           uuid primary key,
    patient_id   uuid not null references patients(id),
    doctor_id    uuid not null references doctors(id),
    procedure_id uuid not null references procedures(id),
    starts_at    timestamptz not null,
    ends_at      timestamptz not null check (ends_at > starts_at),
    status       text not null check (status in ('SCHEDULED', 'CANCELLED')),
    created_at   timestamptz not null
);

-- The rule this whole table exists to enforce, and the only place it actually
-- is enforced: two SCHEDULED rows for the same doctor may not have
-- overlapping [starts_at, ends_at) ranges. A check in application code would
-- still race under concurrent booking — two requests can both read "free"
-- before either writes — because a SELECT and an INSERT are not atomic
-- together. This constraint is, by construction: Postgres evaluates it as
-- part of the same write.
--
-- Scoped to SCHEDULED only, so cancelling an appointment (AppointmentService)
-- actually frees the slot rather than leaving a cancelled row that still
-- blocks it.
alter table appointments
    add constraint no_double_booking
    exclude using gist (
        doctor_id with =,
        tstzrange(starts_at, ends_at) with &&
    ) where (status = 'SCHEDULED');

create index idx_appointments_patient_id on appointments (patient_id);
