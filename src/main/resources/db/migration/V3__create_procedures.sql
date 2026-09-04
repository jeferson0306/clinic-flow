create table procedures (
    id               uuid primary key,
    name             text not null,
    duration_minutes integer not null check (duration_minutes > 0),
    price_cents      bigint not null check (price_cents > 0)
);
