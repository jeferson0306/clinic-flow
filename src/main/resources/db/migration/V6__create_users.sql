create table users (
    id            uuid primary key,
    username      text not null unique,
    password_hash text not null,
    role          text not null check (role in ('ADMIN', 'DOCTOR'))
);

-- The two demo accounts a public sandbox login needs to be usable on first
-- deploy. Real bcrypt hashes, real credentials — the passwords are simply
-- published (README, AuthResource's OpenAPI examples), the same as any
-- other public demo login. Rotate or remove these before this ever protects
-- anything that is not itself a demo.
insert into users (id, username, password_hash, role) values
    (gen_random_uuid(), 'admin',  '$2a$10$/ttwAQ93YdKH1mGO8qoEXuUQnVAYLFry35yuU24cw6oh6jJS59rJC', 'ADMIN'),
    (gen_random_uuid(), 'doctor', '$2a$10$QWFkW8Fw44tkiQYO3PKOhuIqUf3eztMJ..fkXlIR3Z/SDUBG1n4.2', 'DOCTOR');
