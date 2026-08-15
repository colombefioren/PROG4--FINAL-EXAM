alter table "user" add column is_deleted boolean not null default false;

alter table promotion add column is_deleted boolean not null default false;

alter table "group" add column is_deleted boolean not null default false;

alter table exam add column is_deleted boolean not null default false;