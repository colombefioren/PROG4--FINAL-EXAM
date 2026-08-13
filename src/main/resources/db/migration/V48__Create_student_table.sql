create table if not exists student
(
    id           uuid primary key references "user" (id),
    std          varchar(255) not null,
    promotion_id uuid         not null references promotion (id),
    constraint student_std_uk unique (std)
);