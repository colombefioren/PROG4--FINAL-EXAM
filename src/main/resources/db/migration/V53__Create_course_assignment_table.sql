create table if not exists course_assignment
(
    id            uuid primary key default uuid_generate_v4(),
    course_id     uuid        not null references "course" (id),
    group_id      uuid        not null references "group" (id),
    teacher_id    uuid        not null references teacher (id),
    academic_year integer     not null,
    semester      integer     not null,
    credits       integer     not null,
    is_deleted    boolean     not null default false,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    constraint course_assignment_uk unique (course_id, teacher_id, group_id, academic_year, semester)
);