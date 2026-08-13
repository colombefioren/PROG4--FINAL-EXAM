create extension if not exists "uuid-ossp";

create table if not exists course
(
    id   uuid primary key default uuid_generate_v4(),
    code varchar(20)  not null,
    name varchar(255) not null,
    credits      integer not null,
    constraint course_code_uk unique (code)
);