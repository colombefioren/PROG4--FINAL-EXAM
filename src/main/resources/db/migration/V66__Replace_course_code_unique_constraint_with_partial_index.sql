-- A course is soft-deleted (is_deleted) rather than removed, so the plain unique
-- constraint on (code) would block re-creating a course with the same code after a
-- deletion (the soft-deleted row is still physically present). Replace it with a
-- partial unique index that only covers live rows.
alter table course drop constraint course_code_uk;

create unique index course_code_active_uk
    on course (code)
    where not is_deleted;