-- A course assignment is soft-deleted (is_deleted) rather than removed, so the
-- plain unique constraint on (course_id, group_id, academic_year, semester)
-- would block re-assigning the same course to the same group after a deletion
-- (the soft-deleted row is still physically present). Replace it with a partial
-- unique index that only covers live rows, mirroring V63 for grades.
alter table course_assignment drop constraint course_assignment_uk;

create unique index course_assignment_active_uk
    on course_assignment (course_id, group_id, academic_year, semester)
    where not is_deleted;