create table if not exists grade_history
(
    id             uuid primary key default uuid_generate_v4(),
    grade_id       uuid          not null references grade (id),
    previous_value numeric(4, 2) not null,
    new_value      numeric(4, 2) not null,
    reason         varchar(1000) not null,
    changed_by     uuid          not null references "user" (id),
    changed_at     timestamptz   not null default now(),
    created_at     timestamptz   not null default now()
);

create index if not exists grade_history_grade_idx on grade_history (grade_id, changed_at desc);