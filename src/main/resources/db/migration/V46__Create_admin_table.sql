create table if not exists admin
(
    id uuid primary key references "user" (id)
);