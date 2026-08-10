-- Dev seed data (CLAUDE.md §5.2: "~10-15 cakes loaded via data.sql on
-- startup for dev"). Runs on every startup after Hibernate creates/updates
-- the schema (spring.jpa.defer-datasource-initialization=true in
-- config-repo/catalog-service.properties). The DELETE keeps this idempotent
-- across restarts instead of appending duplicate rows each time.
DELETE FROM cakes;

-- created_at is set explicitly (NOW()) because @CreationTimestamp only fires
-- when Hibernate performs the insert; this raw SQL script bypasses Hibernate
-- entirely, and Hibernate generates the column NOT NULL with no DB-level
-- default (it assumes the ORM always populates it) — so under MySQL's
-- strict mode, omitting it here fails with "Field 'created_at' doesn't have
-- a default value".
INSERT INTO cakes (name, description, category, price, available, image_url, created_at) VALUES
('Chocolate Truffle', 'Rich dark chocolate sponge layered with truffle ganache', 'chocolate', 500.00, true, NULL, NOW()),
('Classic Red Velvet', 'Velvety red sponge with cream cheese frosting', 'classic', 550.00, true, NULL, NOW()),
('Vanilla Bean Delight', 'Light vanilla sponge with Madagascar bean buttercream', 'vanilla', 450.00, true, NULL, NOW()),
('Black Forest', 'Chocolate sponge, cherries, and whipped cream', 'chocolate', 520.00, true, NULL, NOW()),
('Lemon Zest', 'Citrus sponge with tangy lemon curd filling', 'fruit', 480.00, true, NULL, NOW()),
('Strawberry Shortcake', 'Vanilla sponge layered with fresh strawberries and cream', 'fruit', 490.00, true, NULL, NOW()),
('Salted Caramel', 'Caramel sponge with a salted caramel drizzle', 'caramel', 560.00, true, NULL, NOW()),
('Carrot Walnut', 'Spiced carrot sponge with walnuts and cream cheese icing', 'classic', 470.00, true, NULL, NOW()),
('Double Chocolate Fudge', 'Two layers of fudge cake with chocolate ganache', 'chocolate', 600.00, true, NULL, NOW()),
('Pineapple Cream', 'Vanilla sponge with pineapple chunks and fresh cream', 'fruit', 460.00, true, NULL, NOW()),
('Pistachio Rose', 'Pistachio sponge with a hint of rose water', 'specialty', 650.00, true, NULL, NOW()),
('Coffee Walnut', 'Coffee-infused sponge with walnut buttercream', 'specialty', 530.00, true, NULL, NOW()),
('Blueberry Cheesecake', 'Baked cheesecake topped with blueberry compote', 'cheesecake', 580.00, true, NULL, NOW()),
('Mango Passion', 'Mango mousse cake with passionfruit glaze', 'fruit', 510.00, false, NULL, NOW());
