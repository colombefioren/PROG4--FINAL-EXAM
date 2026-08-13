create table if not exists grade
(
    id         uuid primary key default uuid_generate_v4(),
    student_id uuid          not null references student (id),
    exam_id    uuid          not null references exam (id),
    value      numeric(4, 2) not null,
    comment    varchar(1000),
    created_at timestamptz   not null default now(),
    updated_at timestamptz   not null default now(),
    constraint grade_student_exam_uk unique (student_id, exam_id),
    constraint grade_value_ck check (value >= 0 and value <= 20)
);