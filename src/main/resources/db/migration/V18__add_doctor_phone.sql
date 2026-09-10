-- Doctor never had a phone field, unlike Patient — an inconsistency between
-- the two nearest entities in this system, not a deliberate omission.
-- Optional, same as Patient.phone.
alter table doctors add column phone text;
