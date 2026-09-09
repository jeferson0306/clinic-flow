-- Continues V7's seed: appointments and exams were the two tables a fresh
-- deploy still had completely empty, which made the calendar, appointments
-- list and exams list look broken rather than merely unpopulated. Looked up
-- by each row's natural key (cpf, license_number, procedure name) rather
-- than a fixed uuid, since V7 generates those with gen_random_uuid() and a
-- later migration cannot know them in advance.

with
  jp as (select id from patients where cpf = '33311122240'),
  fr as (select id from patients where cpf = '10203040570'),
  rm as (select id from patients where cpf = '71243568909'),
  ana as (select id from doctors where license_number = 'CRM-SP 123456'),
  carlos as (select id from doctors where license_number = 'CRM-RJ 234567'),
  mariana as (select id from doctors where license_number = 'CRM-MG 345678'),
  rotina as (select id from procedures where name = 'Consulta de rotina'),
  ecg as (select id from procedures where name = 'Eletrocardiograma'),
  derma as (select id from procedures where name = 'Avaliacao dermatologica')
insert into appointments (id, patient_id, doctor_id, procedure_id, starts_at, ends_at, status, created_at)
select gen_random_uuid(), jp.id, ana.id, rotina.id,
       date_trunc('day', now()) - interval '2 days' + interval '10 hours',
       date_trunc('day', now()) - interval '2 days' + interval '10 hours 30 minutes',
       'SCHEDULED', now() - interval '5 days'
from jp, ana, rotina
union all
select gen_random_uuid(), fr.id, carlos.id, derma.id,
       date_trunc('day', now()) + interval '1 day' + interval '14 hours',
       date_trunc('day', now()) + interval '1 day' + interval '14 hours 40 minutes',
       'SCHEDULED', now() - interval '1 day'
from fr, carlos, derma
union all
select gen_random_uuid(), rm.id, mariana.id, rotina.id,
       date_trunc('day', now()) + interval '3 days' + interval '9 hours',
       date_trunc('day', now()) + interval '3 days' + interval '9 hours 30 minutes',
       'SCHEDULED', now() - interval '2 hours'
from rm, mariana, rotina
union all
select gen_random_uuid(), jp.id, carlos.id, ecg.id,
       date_trunc('day', now()) - interval '5 days' + interval '11 hours',
       date_trunc('day', now()) - interval '5 days' + interval '11 hours 25 minutes',
       'CANCELLED', now() - interval '6 days'
from jp, carlos, ecg;

with
  jp as (select id from patients where cpf = '33311122240'),
  fr as (select id from patients where cpf = '10203040570'),
  rm as (select id from patients where cpf = '71243568909'),
  ana as (select id from doctors where license_number = 'CRM-SP 123456'),
  carlos as (select id from doctors where license_number = 'CRM-RJ 234567'),
  mariana as (select id from doctors where license_number = 'CRM-MG 345678')
insert into exams (id, patient_id, requested_by_doctor_id, type, requested_at, result, result_recorded_at)
select gen_random_uuid(), jp.id, ana.id, 'Hemograma completo',
       now() - interval '4 days', 'Valores dentro da normalidade.', now() - interval '3 days'
from jp, ana
union all
select gen_random_uuid(), fr.id, carlos.id, 'Eletrocardiograma',
       now() - interval '2 days', 'Ritmo sinusal, sem alteracoes.', now() - interval '1 day'
from fr, carlos
union all
select gen_random_uuid(), rm.id, mariana.id, 'Exame de sangue completo',
       now() - interval '1 day', null, null
from rm, mariana;
