create table if not exists exam
(
    id                    uuid primary key default uuid_generate_v4(),
    course_assignment_id  uuid          not null references course_assignment (id),
    title                 varchar(255)  not null,
    exam_datetime         timestamptz   not null,
    coefficient_numerator   integer      not null,
    coefficient_denominator integer      not null,
    created_at              timestamptz  not null default now(),
    updated_at              timestamptz  not null default now(),
    constraint exam_coefficient_denominator_ck check (coefficient_denominator > 0),
    constraint exam_coefficient_ck check (coefficient_numerator > 0 and coefficient_numerator <= coefficient_denominator)
);