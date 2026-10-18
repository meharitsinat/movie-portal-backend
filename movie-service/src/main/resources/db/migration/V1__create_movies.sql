create table movies (
    id uuid primary key,
    title varchar(200) not null,
    year integer,
    genre varchar(100),
    poster varchar(2048),
    description text,
    is_active boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index movies_active_title_idx on movies (is_active, title);
