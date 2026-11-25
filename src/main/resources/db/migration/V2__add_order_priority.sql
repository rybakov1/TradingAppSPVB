ALTER TABLE orders
    ADD COLUMN priority_at TIMESTAMP WITH TIME ZONE;

UPDATE orders
SET priority_at = created_at;

ALTER TABLE orders
    ALTER COLUMN priority_at SET NOT NULL;

ALTER TABLE orders
    ALTER COLUMN priority_at SET DEFAULT NOW();

DROP INDEX idx_orders_buy_matching;
DROP INDEX idx_orders_sell_matching;

CREATE INDEX idx_orders_buy_matching
    ON orders (
               seccode,
               price DESC,
               priority_at ASC,
               id ASC
        )
    WHERE side = 'BUY'
AND status IN ('NEW', 'PARTIALLY_FILLED');

CREATE INDEX idx_orders_sell_matching
    ON orders (
               seccode,
               price ASC,
               priority_at ASC,
               id ASC
        )
    WHERE side = 'SELL'
AND status IN ('NEW', 'PARTIALLY_FILLED');