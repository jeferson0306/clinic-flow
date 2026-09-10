-- V18 added doctors.phone and V17 added patients.complement — both
-- nullable, both never backfilled, so every seeded row demoing those
-- fields showed the same blank UI regardless of which record you opened.
-- This fills in a realistic subset (not all — an untouched null phone is
-- itself a legitimate, common real-world case worth keeping visible),
-- exercising both fields against the actual live app rather than only
-- against a freshly-registered record made during a demo session.
update doctors set phone = '11987654321' where email = 'ana.ferreira@clinicflow.dev';
update doctors set phone = '21976543210' where email = 'carlos.santos@clinicflow.dev';
update doctors set phone = '31985274163' where email = 'mariana.lima@clinicflow.dev';
update doctors set phone = '41999887766' where email = 'ricardo.nogueira@clinicflow.dev';
update doctors set phone = '85991234567' where email = 'beatriz.lins@clinicflow.dev';

update patients set complement = 'Apto 62' where email = 'joao.almeida@example.com';
update patients set complement = 'Bloco B' where email = 'fernanda.reis@example.com';
update patients set complement = 'Casa 3, fundos' where email = 'rafael.martins@example.com';
