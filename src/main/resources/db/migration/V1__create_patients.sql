create table patients (
    id          uuid primary key,
    full_name   text not null,
    cpf         varchar(11) not null unique,
    email       text not null,
    phone       text,
    birth_date  date,
    postcode    varchar(8),
    street      text,
    district    text,
    city        text,
    state       varchar(2),
    created_at  timestamptz not null
);
