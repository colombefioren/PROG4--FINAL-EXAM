create table if not exists admin
(
    id        uuid primary key default uuid_generate_v4(),
    firstname varchar(255) not null,
    lastname  varchar(255) not null,
    email     varchar(255) not null,
    password  varchar(255) not null,
    constraint admin_email_uk unique (email)
);