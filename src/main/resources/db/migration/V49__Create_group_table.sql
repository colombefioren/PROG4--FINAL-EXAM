create table if not exists "group"
(
    id           uuid primary key default uuid_generate_v4(),
    promotion_id uuid         not null references promotion (id),
    ref          varchar(255) not null,
    track        track,
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now(),
    constraint group_ref_uk unique (ref)
);