-- Demo dataset for a public sandbox deploy — same reasoning as V6's seeded
-- accounts: a first-time visitor needs something to look at, not an empty
-- shell. CPFs below are real, checksum-valid numbers (mod-11), not
-- placeholders — DocumentValidator would accept every one of them, they are
-- simply never-issued combinations, the same convention brdoc's own test
-- fixtures use.

insert into doctors (id, full_name, cpf, email, specialty, license_number, created_at) values
    (gen_random_uuid(), 'Ana Beatriz Ferreira',  '12945678888', 'ana.ferreira@clinicflow.dev',   'Cardiologia',  'CRM-SP 123456', now()),
    (gen_random_uuid(), 'Carlos Eduardo Santos', '98765432100', 'carlos.santos@clinicflow.dev',  'Dermatologia', 'CRM-RJ 234567', now()),
    (gen_random_uuid(), 'Mariana Costa Lima',    '55443322150', 'mariana.lima@clinicflow.dev',   'Pediatria',    'CRM-MG 345678', now());

insert into patients (id, full_name, cpf, email, phone, birth_date, postcode, street, district, city, state, created_at) values
    (gen_random_uuid(), 'Joao Pedro Almeida',    '33311122240', 'joao.almeida@example.com',   '11987654321', '1990-04-12', '01310200', 'Avenida Paulista',      'Bela Vista',      'Sao Paulo',      'SP', now()),
    (gen_random_uuid(), 'Fernanda Oliveira Reis','10203040570', 'fernanda.reis@example.com',  '21976543210', '1985-09-23', '22041001', 'Avenida Atlantica',     'Copacabana',      'Rio de Janeiro', 'RJ', now()),
    (gen_random_uuid(), 'Rafael Souza Martins',  '71243568909', 'rafael.martins@example.com', null,          '2001-01-30', '30130010', 'Avenida Afonso Pena',   'Centro',          'Belo Horizonte', 'MG', now());

insert into procedures (id, name, duration_minutes, price_cents) values
    (gen_random_uuid(), 'Consulta de rotina',           30, 15000),
    (gen_random_uuid(), 'Exame de sangue completo',     20,  9000),
    (gen_random_uuid(), 'Eletrocardiograma',            25, 12000),
    (gen_random_uuid(), 'Consulta de retorno',          20,  8000),
    (gen_random_uuid(), 'Avaliacao dermatologica',      40, 18000);
