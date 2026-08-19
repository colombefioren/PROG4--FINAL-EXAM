



alter table grade drop constraint grade_student_exam_uk;

create unique index grade_student_exam_active_uk
    on grade (student_id, exam_id)
    where not is_deleted;
