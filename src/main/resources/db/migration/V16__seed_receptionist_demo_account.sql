-- The fourth demo login — V15 added the RECEPCAO role to every relevant
-- @RolesAllowed but never actually seeded an account for it, unlike ADMIN,
-- DOCTOR (both V6/V11) and PACIENTE (V15 itself). No patient_id: only a
-- PACIENTE login is ever linked to a patient row.
insert into users (id, email, password_hash, role)
values (gen_random_uuid(), 'recepcao@clinicflow.dev',
        '$2a$10$gen.pxEwl7.adfUdvUBtvONFx4Hgk8oWK7KhRvq71I3xrFVKkUp5G', 'RECEPCAO');
