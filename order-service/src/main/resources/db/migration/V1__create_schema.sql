CREATE TABLE orders (
    id               BIGSERIAL PRIMARY KEY,
    customer_email   VARCHAR(160)   NOT NULL,
    status           VARCHAR(20)    NOT NULL,
    total            NUMERIC(12, 2) NOT NULL,
    rejection_reason VARCHAR(255),
    created_at       TIMESTAMPTZ    NOT NULL
);

CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id   BIGINT         NOT NULL,
    product_name VARCHAR(120)   NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    quantity     INTEGER        NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_orders_customer ON orders (customer_email);
CREATE INDEX idx_order_items_order ON order_items (order_id);
