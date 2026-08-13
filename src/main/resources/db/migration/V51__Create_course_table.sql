create table if not exists "course"
(
    id            uuid primary key default uuid_generate_v4(),
    code          varchar(255) not null,
    name          varchar(255) not null,
    credits       integer      not null,
    total_hours   integer,
    student_level student_level,
    track         track,
    is_deleted    boolean      not null default false,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now(),
    constraint course_code_uk unique (code)
);