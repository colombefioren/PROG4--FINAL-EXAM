create table if not exists exam
(
    id                    uuid primary key default uuid_generate_v4(),
    course_assignment_id  uuid          not null references course_assignment (id),
    title                 varchar(255)  not null,
    exam_datetime         timestamptz   not null,
    coefficient           numeric(5, 4) not null,
    created_at            timestamptz   not null default now(),
    updated_at            timestamptz   not null default now(),
    constraint exam_coefficient_ck check (coefficient > 0 and coefficient <= 1)
);