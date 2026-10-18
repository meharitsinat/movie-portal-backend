alter table guest_orders
    add column stripe_checkout_session_id varchar(255);

create unique index guest_orders_stripe_session_idx
    on guest_orders (stripe_checkout_session_id);
