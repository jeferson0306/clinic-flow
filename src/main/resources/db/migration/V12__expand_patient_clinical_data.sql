-- Clinical and legal-guardian fields for a patient record, plus the IBGE
-- municipality code ViaCEP already returns and this app previously ignored.
-- Everything here is nullable: existing patients stay valid with no
-- backfill, and the "guardian required for a minor" rule is enforced by
-- CreatePatientRequest/UpdatePatientRequest's own validation, not the schema.
alter table patients
    add column social_name              text,
    add column mother_name              text,
    add column sex                      varchar(20),
    add column blood_type               varchar(10),
    add column allergies                text,
    add column continuous_medications   text,
    add column pre_existing_conditions  text,
    add column clinical_alert           text,
    add column guardian_name            text,
    add column guardian_cpf             varchar(11),
    add column guardian_relationship    varchar(10),
    add column guardian_phone           text,
    add column ibge_code                varchar(7);
