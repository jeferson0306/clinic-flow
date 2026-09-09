alter table users drop constraint if exists users_role_check;
alter table users add constraint users_role_check
    check (role in ('ADMIN', 'DOCTOR', 'RECEPCAO', 'PACIENTE'));

alter table users add column patient_id uuid references patients (id);

-- A demo patient dedicated to the portal login — not one of V10's
-- generated patients, so the published email/password pair always points
-- at one predictable, stable record.
insert into patients (id, full_name, cpf, email, postcode, street, district, city, state, created_at)
values ('99999999-9999-9999-9999-999999999999', 'Paciente Demo', '47706543323',
        'paciente-demo@example.com', '01310200', 'Avenida Paulista', 'Bela Vista',
        'São Paulo', 'SP', now());

insert into users (id, email, password_hash, role, patient_id)
values (gen_random_uuid(), 'paciente@clinicflow.dev',
        '$2a$10$5DIdkovVSjnMfjedtwCFl.1g3mw9N6lAAK3DjGmGlFDCvi8Fjc3Na', 'PACIENTE',
        '99999999-9999-9999-9999-999999999999');
