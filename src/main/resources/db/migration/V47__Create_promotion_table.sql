create table if not exists promotion
(
    id         uuid primary key default uuid_generate_v4(),
    ref        varchar(255) not null,
    name       varchar(255) not null,
    entry_year integer      not null,
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now(),
    constraint promotion_ref_uk unique (ref),
    constraint promotion_name_uk unique (name)
);