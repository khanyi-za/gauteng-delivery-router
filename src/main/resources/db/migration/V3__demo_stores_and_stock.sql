-- Demo data: eight stores at real Gauteng shopping centres, eight products, and stock levels.

INSERT INTO store (name, lat, lon) VALUES
                                       ('Sandton',        -26.1086, 28.0527),
                                       ('Rosebank',       -26.1460, 28.0430),
                                       ('Mall of Africa', -26.0158, 28.1077),
                                       ('Menlyn',         -25.7838, 28.2766),
                                       ('Eastgate',       -26.1805, 28.1167),
                                       ('Clearwater',     -26.1270, 27.9030),
                                       ('Soweto',         -26.2600, 27.8920),
                                       ('Centurion',      -25.8580, 28.1890);

INSERT INTO product (sku, name) VALUES
                                    ('TSHIRT-WHT-M',  'White T-shirt, size M'),
                                    ('JEANS-BLU-32',  'Blue jeans, waist 32'),
                                    ('DRESS-FLR-S',   'Floral dress, size S'),
                                    ('SNEAKER-WHT-8', 'White sneakers, size 8'),
                                    ('JACKET-DNM-L',  'Denim jacket, size L'),
                                    ('SCARF-SILK',    'Silk scarf'),
                                    ('HAT-BUCKET',    'Bucket hat'),
                                    ('BAG-TOTE',      'Canvas tote bag');

-- Every store starts with 5 of everything...
INSERT INTO store_stock (store_id, sku, quantity)
SELECT store.id, product.sku, 5 FROM store CROSS JOIN product;

-- ...except these, so the nearest store doesn't always win
UPDATE store_stock SET quantity = 0
    FROM store
WHERE store.id = store_stock.store_id
  AND (store.name, store_stock.sku) IN (
    ('Sandton',        'SNEAKER-WHT-8'),
    ('Sandton',        'JACKET-DNM-L'),
    ('Rosebank',       'SNEAKER-WHT-8'),
    ('Eastgate',       'DRESS-FLR-S'),
    ('Mall of Africa', 'SCARF-SILK'));

UPDATE store_stock SET quantity = 1
    FROM store
WHERE store.id = store_stock.store_id
  AND store.name = 'Menlyn' AND store_stock.sku = 'BAG-TOTE';