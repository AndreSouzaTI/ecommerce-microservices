CREATE TABLE products (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(120)   NOT NULL,
    price          NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    stock_quantity INTEGER        NOT NULL CHECK (stock_quantity >= 0),
    version        BIGINT         NOT NULL DEFAULT 0
);

CREATE TABLE processed_orders (
    order_id     BIGINT PRIMARY KEY,
    reserved     BOOLEAN     NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

INSERT INTO products (name, price, stock_quantity) VALUES
    ('Notebook Pro 14', 5499.90, 10),
    ('Monitor 27 4K', 2199.00, 25),
    ('Teclado Mecânico', 459.90, 40),
    ('Mouse Sem Fio', 249.90, 60);
