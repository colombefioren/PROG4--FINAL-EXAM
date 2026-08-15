create table if not exists course_assignment_teacher
(
    course_assignment_id uuid not null references course_assignment (id),
    teacher_id           uuid not null references teacher (id),
    primary key (course_assignment_id, teacher_id)
);

insert into course_assignment_teacher (course_assignment_id, teacher_id)
select id, teacher_id
from course_assignment
where teacher_id is not null;

alter table course_assignment drop constraint course_assignment_uk;
alter table course_assignment drop column teacher_id;
alter table course_assignment
    add constraint course_assignment_uk unique (course_id, group_id, academic_year, semester);
