create table if not exists teacher
(
    id uuid primary key references "user" (id)
);