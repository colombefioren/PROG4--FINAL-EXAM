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

create table if not exists teacher
(
    id uuid primary key references "user" (id)
);

create table if not exists admin
(
    id uuid primary key references "user" (id)
);

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

create table if not exists student
(
    id           uuid primary key references "user" (id),
    std          varchar(255) not null,
    promotion_id uuid         not null references promotion (id),
    constraint student_std_uk unique (std)
);
