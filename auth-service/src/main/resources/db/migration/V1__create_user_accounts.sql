create table user_accounts (
    id uuid primary key,
    name varchar(120) not null,
    email varchar(320) not null,
    password_hash varchar(100),
    role varchar(16) not null check (role in ('GUEST', 'ADMIN')),
    created_at timestamp with time zone not null
);

create index user_accounts_email_role_idx on user_accounts (email, role);
