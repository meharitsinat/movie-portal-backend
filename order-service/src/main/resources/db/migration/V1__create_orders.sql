create table guest_orders (
    id uuid primary key,
    guest_id uuid not null,
    guest_name varchar(120) not null,
    guest_email varchar(320) not null,
    price numeric(10, 2) not null default 0 check (price >= 0),
    status varchar(24) not null check (status in ('PENDING', 'CHECKOUT_READY', 'PAID', 'CANCELLED')),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index guest_orders_guest_created_idx on guest_orders (guest_id, created_at desc);
create index guest_orders_status_idx on guest_orders (status);

create table order_movies (
    id uuid primary key,
    order_id uuid not null references guest_orders(id) on delete cascade,
    movie_id uuid not null,
    unique (order_id, movie_id)
);

create index order_movies_order_id_idx on order_movies (order_id);
create index order_movies_movie_id_idx on order_movies (movie_id);
