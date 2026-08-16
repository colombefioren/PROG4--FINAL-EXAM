-- A grade is soft-deleted (is_deleted) rather than removed, so the plain unique
-- constraint on (student_id, exam_id) would block re-grading the same student+exam
-- after a deletion (the soft-deleted row is still physically present). Replace it
-- with a partial unique index that only covers live rows.
alter table grade drop constraint grade_student_exam_uk;

create unique index grade_student_exam_active_uk
    on grade (student_id, exam_id)
    where not is_deleted;
