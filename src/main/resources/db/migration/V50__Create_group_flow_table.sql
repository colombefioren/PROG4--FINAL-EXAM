create table if not exists group_flow
(
    id              uuid primary key default uuid_generate_v4(),
    student_id      uuid           not null references student (id),
    group_id        uuid           not null references "group" (id),
    group_flow_type group_flow_type,
    created_at      timestamptz    not null default now()
);