create table if not exists "user"
(
    id         uuid primary key default uuid_generate_v4(),
    firstname  varchar(255) not null,
    lastname   varchar(255) not null,
    email      varchar(255) not null,
    password   varchar(255) not null,
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now(),
    constraint user_email_uk unique (email)
);