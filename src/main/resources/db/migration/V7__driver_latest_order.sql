-- The order from each driver's latest trip, so the live map can work out where they are on it.
-- If that order is deleted, the driver just loses the link.

ALTER TABLE driver
    ADD COLUMN latest_order_id BIGINT REFERENCES delivery_order (id) ON DELETE SET NULL;

-- Fill it in for drivers who already made deliveries before this column existed
UPDATE driver
SET latest_order_id = (SELECT o.id
                       FROM delivery_order o
                       WHERE o.driver_id = driver.id
                       ORDER BY o.dispatched_at DESC
                       LIMIT 1);
