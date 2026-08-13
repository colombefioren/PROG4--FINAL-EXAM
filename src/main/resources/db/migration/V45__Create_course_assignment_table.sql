create table if not exists course_assignment
(
    id                  uuid primary key default uuid_generate_v4(),
    promotion_course_id uuid    not null references promotion_course (id),
    group_id            uuid    not null references student_group (id),
    teacher_id          uuid    not null references teacher (id),
    academic_year       integer not null,
    constraint course_assignment_uk unique (promotion_course_id, group_id, teacher_id, academic_year)
);