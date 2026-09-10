-- phone and birth_date move from optional to required: a clinic cannot
-- reschedule an appointment, deliver an exam result, or confirm identity
-- without a working phone number, and birth_date is not just demographic
-- color — RequiresGuardianIfMinor only ever runs its guardian check when
-- birth_date is present ("a null birthDate is not treated as definitely a
-- minor" is its own javadoc's words), so an optional birth_date has been a
-- real way to register a minor with zero guardian information on file the
-- whole time. Both become NOT NULL here, backed by CreatePatientRequest/
-- UpdatePatientRequest now requiring them too (see RequiresGuardianIfMinor
-- and the two request records).
--
-- Backfill first, same reasoning as V20's house_number backfill: a
-- deterministic, varied, plausible value per row, not a single repeated
-- placeholder. Phone gets each patient's own state's real DDD (area code)
-- plus a hash-derived subscriber number; birth_date (only V15's single
-- "Paciente Demo" row lacks one) gets a fixed plausible adult date.
update patients
set phone = (
  case state
    when 'SP' then '11' when 'RJ' then '21' when 'MG' then '31'
    when 'ES' then '27' when 'DF' then '61' when 'GO' then '62'
    when 'BA' then '71' when 'PE' then '81' when 'CE' then '85'
    when 'AM' then '92' when 'PA' then '91' when 'PR' then '41'
    when 'SC' then '48' when 'RS' then '51' else '11'
  end
) || '9' || lpad((abs(('x' || substr(md5(id::text), 1, 8))::bit(32)::int) % 100000000)::text, 8, '0')
where phone is null;

update patients set birth_date = date '1990-06-15' where birth_date is null;

alter table patients
    alter column phone set not null,
    alter column birth_date set not null;
