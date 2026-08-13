create extension if not exists "uuid-ossp";

create type student_level as enum ('L1', 'L2', 'L3');
create type track as enum ('EL', 'TN');
create type group_flow_type as enum ('JOIN', 'LEAVE');
