create table exams (
    id                     uuid primary key,
    patient_id             uuid not null references patients(id),
    requested_by_doctor_id uuid not null references doctors(id),
    type                   text not null,
    requested_at           timestamptz not null,
    result                 text,
    result_recorded_at     timestamptz
);

-- The one query this module makes beyond fetch-by-id: a patient's exam
-- history. Nothing yet queries by doctor, so no index for that side.
create index idx_exams_patient_id on exams (patient_id);
