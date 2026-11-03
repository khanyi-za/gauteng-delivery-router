-- Demo data: two drivers at every store, so a busy store has to choose which order goes first.

INSERT INTO driver (store_id, name)
SELECT store.id, store.name || ' driver ' || number
FROM store CROSS JOIN generate_series(1, 2) AS number;