-- The four seeded demo accounts (V6/V11/V15/V16) predate PasswordPolicy's
-- current 12-character minimum — "admin123" and friends were never wrong
-- to write at the time, but PasswordPolicy only ever validates a password
-- someone is *setting* (registration, password change), never one already
-- on file, so tightening that policy later did nothing to these existing
-- hashes. A portfolio demo showcasing password strength enforcement
-- shouldn't have "admin123" as its own flagship login. New hashes below,
-- generated the same way as V16's (BcryptUtil.bcryptHash), each 15+
-- characters with upper/lower/digit/special — the demo accounts now meet
-- the same bar PasswordPolicy enforces for everyone else.
update users set password_hash = '$2a$10$wIvm0Swq8BK3wIe3R.M4Zua0CKnHC53mA9MuuGXxCfynq.a8u5MTS'
  where email = 'admin@clinicflow.dev';
update users set password_hash = '$2a$10$NlGRO/GuSpR2eqb/XAvmjewuMsEg21iBkgoLrNbggyxeMYicZFGWa'
  where email = 'doctor@clinicflow.dev';
update users set password_hash = '$2a$10$zYXI2zKge5yGeE0JOaVe7uu4kaJAJb0SP7ONdnQiSAZAqnggkAuPS'
  where email = 'recepcao@clinicflow.dev';
update users set password_hash = '$2a$10$lsrwRuR6tFY4rM8p3VoijeqsNJI/BJZgdWSp.wWHF7ZITxgeDXZB.'
  where email = 'paciente@clinicflow.dev';
