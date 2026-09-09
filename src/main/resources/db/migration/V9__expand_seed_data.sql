-- V7/V8 gave the demo dataset just enough to not look empty (3 patients, 3
-- doctors, 5 procedures) — too little to show off search, sorting or
-- pagination doing real work. This adds volume: 5 more doctors across new
-- specialties, 12 more patients across different Brazilian states/postcodes,
-- 5 more procedures, and more appointments/exams spanning both the old and
-- new rows. Same convention as V7: CPFs are real, checksum-valid numbers
-- that are simply never-issued combinations, not placeholders.

insert into doctors (id, full_name, cpf, email, specialty, license_number, created_at) values
    (gen_random_uuid(), 'Ricardo Alves Nogueira', '44152637803', 'ricardo.nogueira@clinicflow.dev', 'Ortopedia',      'CRM-SP 456789', now()),
    (gen_random_uuid(), 'Beatriz Correia Lins',   '60933714254', 'beatriz.lins@clinicflow.dev',     'Ginecologia',    'CRM-RJ 567890', now()),
    (gen_random_uuid(), 'Fernando Pires Duarte',  '83719426564', 'fernando.duarte@clinicflow.dev',  'Oftalmologia',   'CRM-DF 678901', now()),
    (gen_random_uuid(), 'Camila Rocha Teixeira',  '27491638031', 'camila.teixeira@clinicflow.dev',  'Psiquiatria',    'CRM-BA 789012', now()),
    (gen_random_uuid(), 'Eduardo Barros Cunha',   '59268147319', 'eduardo.cunha@clinicflow.dev',    'Endocrinologia', 'CRM-PR 890123', now());

insert into patients (id, full_name, cpf, email, phone, birth_date, postcode, street, district, city, state, created_at) values
    (gen_random_uuid(), 'Larissa Mendes Barbosa',   '14826935773', 'larissa.barbosa@example.com',    '61988112233', '1993-07-14', '70040010', 'Esplanada dos Ministerios',      'Zona Civica',       'Brasilia',       'DF', now()),
    (gen_random_uuid(), 'Thiago Nascimento Farias', '92581470305', 'thiago.farias@example.com',      '71987654321', '1978-02-28', '40020000', 'Avenida Sete de Setembro',       'Centro',            'Salvador',       'BA', now()),
    (gen_random_uuid(), 'Patricia Gomes Vasconcelos','36925814755','patricia.vasconcelos@example.com', null,        '1988-11-05', '80010000', 'Rua XV de Novembro',             'Centro',            'Curitiba',       'PR', now()),
    (gen_random_uuid(), 'Bruno Cardoso Siqueira',   '70369258177', 'bruno.siqueira@example.com',     '51991223344', '1995-05-19', '90010001', 'Avenida Borges de Medeiros',     'Centro Historico',  'Porto Alegre',   'RS', now()),
    (gen_random_uuid(), 'Juliana Ribeiro Andrade',  '05162738417', 'juliana.andrade@example.com',    '81986754321', '1982-09-30', '50030000', 'Rua da Aurora',                  'Boa Vista',         'Recife',         'PE', now()),
    (gen_random_uuid(), 'Diego Martins Pereira',    '81470369222', 'diego.pereira@example.com',      '85987651234', '2000-12-01', '60060000', 'Avenida Beira Mar',              'Praia de Iracema',  'Fortaleza',      'CE', now()),
    (gen_random_uuid(), 'Renata Souza Lopes',       '49506172811', 'renata.lopes@example.com',       '92988776655', '1991-03-22', '69005000', 'Avenida Eduardo Ribeiro',        'Centro',            'Manaus',         'AM', now()),
    (gen_random_uuid(), 'Marcelo Freitas Aguiar',   '63074185244', 'marcelo.aguiar@example.com',     null,          '1975-08-08', '66017000', 'Avenida Presidente Vargas',      'Campina',           'Belem',          'PA', now()),
    (gen_random_uuid(), 'Aline Costa Monteiro',     '18529630777', 'aline.monteiro@example.com',     '48991234567', '1997-01-17', '88010400', 'Avenida Beira Mar Norte',        'Centro',            'Florianopolis',  'SC', now()),
    (gen_random_uuid(), 'Gustavo Henrique Ramos',   '29630741822', 'gustavo.ramos@example.com',      '11976543210', '1984-06-25', '04538133', 'Avenida Brigadeiro Faria Lima',  'Itaim Bibi',        'Sao Paulo',      'SP', now()),
    (gen_random_uuid(), 'Vanessa Lima Cavalcante',  '37285194646', 'vanessa.cavalcante@example.com', null,          '1969-10-10', '22250040', 'Rua Voluntarios da Patria',      'Botafogo',          'Rio de Janeiro', 'RJ', now()),
    (gen_random_uuid(), 'Rodrigo Almeida Vieira',   '94713825050', 'rodrigo.vieira@example.com',     '31987654321', '2003-04-03', '30190090', 'Avenida do Contorno',            'Funcionarios',      'Belo Horizonte', 'MG', now());

insert into procedures (id, name, duration_minutes, price_cents) values
    (gen_random_uuid(), 'Consulta ortopedica',    30, 16000),
    (gen_random_uuid(), 'Ultrassom obstetrico',   35, 22000),
    (gen_random_uuid(), 'Exame oftalmologico',    25, 14000),
    (gen_random_uuid(), 'Consulta psiquiatrica',  50, 25000),
    (gen_random_uuid(), 'Exame de urina',         15,  6000);

-- More appointments across both the V7 and this migration's people, so the
-- calendar and appointments list have enough rows to make sorting/paging on
-- them worth demonstrating rather than a handful of entries on one screen.
with
  jp as (select id from patients where cpf = '33311122240'),
  fr as (select id from patients where cpf = '10203040570'),
  larissa as (select id from patients where cpf = '14826935773'),
  thiago as (select id from patients where cpf = '92581470305'),
  bruno as (select id from patients where cpf = '70369258177'),
  juliana as (select id from patients where cpf = '05162738417'),
  gustavo as (select id from patients where cpf = '29630741822'),
  ana as (select id from doctors where license_number = 'CRM-SP 123456'),
  ricardo as (select id from doctors where license_number = 'CRM-SP 456789'),
  beatriz as (select id from doctors where license_number = 'CRM-RJ 567890'),
  fernando as (select id from doctors where license_number = 'CRM-DF 678901'),
  camila as (select id from doctors where license_number = 'CRM-BA 789012'),
  eduardo as (select id from doctors where license_number = 'CRM-PR 890123'),
  rotina as (select id from procedures where name = 'Consulta de rotina'),
  ortopedica as (select id from procedures where name = 'Consulta ortopedica'),
  obstetrico as (select id from procedures where name = 'Ultrassom obstetrico'),
  oftalmo as (select id from procedures where name = 'Exame oftalmologico'),
  psiquiatrica as (select id from procedures where name = 'Consulta psiquiatrica')
insert into appointments (id, patient_id, doctor_id, procedure_id, starts_at, ends_at, status, created_at)
select gen_random_uuid(), larissa.id, ricardo.id, ortopedica.id,
       date_trunc('day', now()) - interval '3 days' + interval '9 hours',
       date_trunc('day', now()) - interval '3 days' + interval '9 hours 30 minutes',
       'SCHEDULED', now() - interval '4 days'
from larissa, ricardo, ortopedica
union all
select gen_random_uuid(), thiago.id, beatriz.id, obstetrico.id,
       date_trunc('day', now()) + interval '2 days' + interval '10 hours',
       date_trunc('day', now()) + interval '2 days' + interval '10 hours 35 minutes',
       'SCHEDULED', now() - interval '3 hours'
from thiago, beatriz, obstetrico
union all
select gen_random_uuid(), bruno.id, fernando.id, oftalmo.id,
       date_trunc('day', now()) + interval '4 days' + interval '15 hours',
       date_trunc('day', now()) + interval '4 days' + interval '15 hours 25 minutes',
       'SCHEDULED', now() - interval '1 day'
from bruno, fernando, oftalmo
union all
select gen_random_uuid(), juliana.id, camila.id, psiquiatrica.id,
       date_trunc('day', now()) - interval '1 day' + interval '16 hours',
       date_trunc('day', now()) - interval '1 day' + interval '16 hours 50 minutes',
       'SCHEDULED', now() - interval '2 days'
from juliana, camila, psiquiatrica
union all
select gen_random_uuid(), gustavo.id, eduardo.id, rotina.id,
       date_trunc('day', now()) + interval '6 days' + interval '11 hours',
       date_trunc('day', now()) + interval '6 days' + interval '11 hours 30 minutes',
       'SCHEDULED', now() - interval '5 hours'
from gustavo, eduardo, rotina
union all
select gen_random_uuid(), jp.id, ricardo.id, ortopedica.id,
       date_trunc('day', now()) - interval '7 days' + interval '13 hours',
       date_trunc('day', now()) - interval '7 days' + interval '13 hours 30 minutes',
       'CANCELLED', now() - interval '8 days'
from jp, ricardo, ortopedica
union all
select gen_random_uuid(), fr.id, eduardo.id, rotina.id,
       date_trunc('day', now()) - interval '10 days' + interval '9 hours',
       date_trunc('day', now()) - interval '10 days' + interval '9 hours 30 minutes',
       'SCHEDULED', now() - interval '11 days'
from fr, eduardo, rotina
union all
select gen_random_uuid(), larissa.id, ana.id, rotina.id,
       date_trunc('day', now()) + interval '8 days' + interval '10 hours',
       date_trunc('day', now()) + interval '8 days' + interval '10 hours 30 minutes',
       'SCHEDULED', now() - interval '2 hours'
from larissa, ana, rotina;

with
  larissa as (select id from patients where cpf = '14826935773'),
  patricia as (select id from patients where cpf = '36925814755'),
  diego as (select id from patients where cpf = '81470369222'),
  renata as (select id from patients where cpf = '49506172811'),
  marcelo as (select id from patients where cpf = '63074185244'),
  aline as (select id from patients where cpf = '18529630777'),
  ricardo as (select id from doctors where license_number = 'CRM-SP 456789'),
  fernando as (select id from doctors where license_number = 'CRM-DF 678901'),
  eduardo as (select id from doctors where license_number = 'CRM-PR 890123'),
  camila as (select id from doctors where license_number = 'CRM-BA 789012')
insert into exams (id, patient_id, requested_by_doctor_id, type, requested_at, result, result_recorded_at)
select gen_random_uuid(), larissa.id, ricardo.id, 'Raio-X do joelho',
       now() - interval '6 days', 'Sem sinais de fratura.', now() - interval '5 days'
from larissa, ricardo
union all
select gen_random_uuid(), patricia.id, fernando.id, 'Exame de acuidade visual',
       now() - interval '3 days', 'Miopia leve, sem necessidade de cirurgia.', now() - interval '2 days'
from patricia, fernando
union all
select gen_random_uuid(), diego.id, eduardo.id, 'Glicemia em jejum',
       now() - interval '2 days', null, null
from diego, eduardo
union all
select gen_random_uuid(), renata.id, camila.id, 'Avaliacao psiquiatrica inicial',
       now() - interval '5 days', 'Acompanhamento recomendado, retorno em 30 dias.', now() - interval '4 days'
from renata, camila
union all
select gen_random_uuid(), marcelo.id, ricardo.id, 'Ressonancia magnetica do ombro',
       now() - interval '1 day', null, null
from marcelo, ricardo
union all
select gen_random_uuid(), aline.id, fernando.id, 'Exame de fundo de olho',
       now() - interval '8 days', 'Sem alteracoes na retina.', now() - interval '7 days'
from aline, fernando;
