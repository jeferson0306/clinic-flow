-- Login moves from a bare "username" to an email address, validated as one
-- at the request layer (LoginRequest.email is @Email now). The two seeded
-- demo accounts are renamed to match rather than left as the one place in
-- the whole system that still looks like a username — same bcrypt hashes,
-- same passwords, only the identifier used to log in changes.
alter table users rename column username to email;

update users set email = 'admin@clinicflow.dev' where email = 'admin';
update users set email = 'doctor@clinicflow.dev' where email = 'doctor';
