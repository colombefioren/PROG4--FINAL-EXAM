-- A user is soft-deleted (is_deleted) rather than removed, so the plain unique
-- constraint on (email) would block re-creating a student/teacher/admin with the
-- same email after a deletion (the soft-deleted row is still physically present).
-- Replace it with a partial unique index that only covers live rows.
alter table "user" drop constraint user_email_uk;

create unique index user_email_active_uk
    on "user" (email)
    where not is_deleted;