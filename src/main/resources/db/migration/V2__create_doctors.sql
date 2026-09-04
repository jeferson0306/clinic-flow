create table doctors (
    id              uuid primary key,
    full_name       text not null,
    cpf             varchar(11) not null unique,
    email           text not null,
    specialty       text not null,
    license_number  text not null unique,
    created_at      timestamptz not null
);
