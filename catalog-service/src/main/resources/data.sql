-- Dev seed data (CLAUDE.md §5.2: "~10-15 cakes loaded via data.sql on
-- startup for dev"). Runs on every startup after Hibernate creates/updates
-- the schema (spring.jpa.defer-datasource-initialization=true in
-- config-repo/catalog-service.properties). The DELETE keeps this idempotent
-- across restarts instead of appending duplicate rows each time.
DELETE FROM cakes;

-- image_url values point at Wikimedia Commons (upload.wikimedia.org) —
-- freely licensed (public domain / CC), stably hosted, and each URL was
-- verified (HTTP 200, image/jpeg) before being added here. Picked for a
-- reasonable visual match to the cake's name/description, not for being
-- the literal product photo. frontend-service's CakeCard/cake detail page
-- render this directly via a plain <img> (see frontend-service/lib/
-- category-style.ts) — no code change needed for these to show up.
--
-- created_at is set explicitly (NOW()) because @CreationTimestamp only fires
-- when Hibernate performs the insert; this raw SQL script bypasses Hibernate
-- entirely, and Hibernate generates the column NOT NULL with no DB-level
-- default (it assumes the ORM always populates it) — so under MySQL's
-- strict mode, omitting it here fails with "Field 'created_at' doesn't have
-- a default value".
INSERT INTO cakes (name, description, category, price, available, image_url, created_at) VALUES
('Chocolate Truffle', 'Rich dark chocolate sponge layered with truffle ganache', 'chocolate', 500.00, true, 'https://upload.wikimedia.org/wikipedia/commons/thumb/f/fc/Chocolate_truffle_cake.jpg/960px-Chocolate_truffle_cake.jpg', NOW()),
('Classic Red Velvet', 'Velvety red sponge with cream cheese frosting', 'classic', 550.00, true, 'https://upload.wikimedia.org/wikipedia/commons/thumb/1/13/Red_velvet_cake_slice.jpg/960px-Red_velvet_cake_slice.jpg', NOW()),
('Vanilla Bean Delight', 'Light vanilla sponge with Madagascar bean buttercream', 'vanilla', 450.00, true, 'https://upload.wikimedia.org/wikipedia/commons/a/a2/Vanilla_Cake.jpg', NOW()),
('Black Forest', 'Chocolate sponge, cherries, and whipped cream', 'chocolate', 520.00, true, 'https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/Black_Forest_cake_4.jpg/960px-Black_Forest_cake_4.jpg', NOW()),
('Lemon Zest', 'Citrus sponge with tangy lemon curd filling', 'fruit', 480.00, true, 'https://upload.wikimedia.org/wikipedia/commons/thumb/d/de/Lemon_Cake_-_Olea_2025-08-17.jpg/960px-Lemon_Cake_-_Olea_2025-08-17.jpg', NOW()),
('Strawberry Shortcake', 'Vanilla sponge layered with fresh strawberries and cream', 'fruit', 490.00, true, 'https://upload.wikimedia.org/wikipedia/commons/thumb/f/f2/Shortcake_slice.jpg/960px-Shortcake_slice.jpg', NOW()),
('Salted Caramel', 'Caramel sponge with a salted caramel drizzle', 'caramel', 560.00, true, 'https://upload.wikimedia.org/wikipedia/commons/thumb/f/f6/Caramel_Cake.jpg/960px-Caramel_Cake.jpg', NOW()),
('Carrot Walnut', 'Spiced carrot sponge with walnuts and cream cheese icing', 'classic', 470.00, true, 'https://upload.wikimedia.org/wikipedia/commons/thumb/2/25/Carrot_cake_-_Milfey_Patisserie_2026-04-04.jpg/960px-Carrot_cake_-_Milfey_Patisserie_2026-04-04.jpg', NOW()),
('Double Chocolate Fudge', 'Two layers of fudge cake with chocolate ganache', 'chocolate', 600.00, true, 'https://upload.wikimedia.org/wikipedia/commons/thumb/9/95/Chocolate_cake_with_ganache_frosting.jpg/960px-Chocolate_cake_with_ganache_frosting.jpg', NOW()),
('Pineapple Cream', 'Vanilla sponge with pineapple chunks and fresh cream', 'fruit', 460.00, true, 'https://upload.wikimedia.org/wikipedia/commons/thumb/8/88/Pineapple_upsidedown_cake_9.jpg/960px-Pineapple_upsidedown_cake_9.jpg', NOW()),
('Pistachio Rose', 'Pistachio sponge with a hint of rose water', 'specialty', 650.00, true, 'https://upload.wikimedia.org/wikipedia/commons/thumb/1/1e/Pistachio_Cake.jpg/960px-Pistachio_Cake.jpg', NOW()),
('Coffee Walnut', 'Coffee-infused sponge with walnut buttercream', 'specialty', 530.00, true, 'https://upload.wikimedia.org/wikipedia/commons/thumb/3/33/A_slice_of_coffee_and_walnut_cake_-_Audit_Room_Caf%C3%A9%2C_Petworth_House.jpg/960px-A_slice_of_coffee_and_walnut_cake_-_Audit_Room_Caf%C3%A9%2C_Petworth_House.jpg', NOW()),
('Blueberry Cheesecake', 'Baked cheesecake topped with blueberry compote', 'cheesecake', 580.00, true, 'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c0/Blueberry_cheesecake_from_Pepper_Stone_Bakery.jpg/960px-Blueberry_cheesecake_from_Pepper_Stone_Bakery.jpg', NOW()),
('Mango Passion', 'Mango mousse cake with passionfruit glaze', 'fruit', 510.00, false, 'https://upload.wikimedia.org/wikipedia/commons/thumb/3/3c/Mango_Passion_cake_%28Red_Ribbon%2C_Philippines%29.jpg/960px-Mango_Passion_cake_%28Red_Ribbon%2C_Philippines%29.jpg', NOW());
