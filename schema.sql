-- ============================================================
-- PAU Cafeteria App — PostgreSQL Schema
-- Run this in pgAdmin or psql against the "paucafe" database
-- ============================================================

-- USERS table
CREATE TABLE IF NOT EXISTS users (
    id          SERIAL PRIMARY KEY,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,  -- BCrypt hashed
    role        VARCHAR(20) DEFAULT 'customer',  -- 'customer' or 'manager'
    created_at  TIMESTAMP DEFAULT NOW()
);

-- MENU ITEMS table
CREATE TABLE IF NOT EXISTS menu_items (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    category    VARCHAR(100) NOT NULL,
    quantity    INT NOT NULL DEFAULT 0,
    price       NUMERIC(10,2) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- ORDERS table
CREATE TABLE IF NOT EXISTS orders (
    id              SERIAL PRIMARY KEY,
    user_email      VARCHAR(255) NOT NULL REFERENCES users(email),
    total_amount    NUMERIC(10,2) NOT NULL,
    status          VARCHAR(50) DEFAULT 'Completed',
    created_at      TIMESTAMP DEFAULT NOW()
);

-- ORDER ITEMS table
CREATE TABLE IF NOT EXISTS order_items (
    id              SERIAL PRIMARY KEY,
    order_id        INT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id    INT NOT NULL REFERENCES menu_items(id),
    quantity        INT NOT NULL,
    unit_price      NUMERIC(10,2) NOT NULL
);

-- ============================================================
-- SEED: Insert manager account
-- Password: Manager@123  (BCrypt hashed below)
-- ============================================================
INSERT INTO users (first_name, last_name, email, password, role)
VALUES (
    'Cafeteria', 'Manager',
    'manager@paucafe.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh32',  -- Manager@123
    'manager'
) ON CONFLICT (email) DO NOTHING;

-- ============================================================
-- SEED: Insert menu items from CSV data
-- ============================================================
INSERT INTO menu_items (id, name, category, quantity, price, description) VALUES
(1,  'Glass (35cl) Soft Drink',      'Drinks',    92, 300.00,  'Chilled assorted soft drinks in glass bottle'),
(2,  'Malta Guinness Can',            'Drinks',    5,  800.00,  'Rich malt drink served in a chilled can'),
(3,  'Malta Guinness Glass',          'Drinks',    19, 800.00,  'Rich malt drink served in a glass bottle'),
(4,  'Zobo Drink (35cl)',             'Drinks',    68, 600.00,  'Chilled hibiscus drink sweetened and spiced'),
(5,  'Water (50cl)',                  'Drinks',    38, 250.00,  'Chilled sachet or bottle water'),
(6,  'Chivita Juice (35cl)',          'Drinks',    44, 500.00,  'Assorted fruit juice in chilled pack'),
(7,  'Jollof Rice',                   'Main Meal', 60, 1500.00, 'Party-style jollof rice served with fried plantain'),
(8,  'Fried Rice',                    'Main Meal', 45, 1500.00, 'Vegetable fried rice with carrots and green peas'),
(9,  'White Rice and Stew',           'Main Meal', 50, 1200.00, 'Plain white rice served with tomato beef stew'),
(10, 'Spaghetti Jollof',              'Main Meal', 30, 1400.00, 'Tomato-based jollof spaghetti with chicken'),
(11, 'Beans and Plantain',            'Main Meal', 35, 1200.00, 'Porridge beans served with fried ripe plantain'),
(12, 'Yam and Egg Sauce',             'Main Meal', 25, 1300.00, 'Boiled yam served with spiced egg sauce'),
(13, 'Eba and Egusi Soup',            'Swallow',   20, 1800.00, 'Eba served with rich egusi soup'),
(14, 'Amala and Ewedu',               'Swallow',   18, 1800.00, 'Amala served with ewedu soup and gbegiri'),
(15, 'Pounded Yam and Ofe Onugbu',    'Swallow',   15, 2000.00, 'Pounded yam served with bitter leaf soup'),
(16, 'Semovita and Okra Soup',        'Swallow',   22, 1800.00, 'Semovita served with draw okra and assorted fish'),
(17, 'Grilled Chicken',               'Protein',   40, 2500.00, 'Seasoned whole chicken leg grilled to perfection'),
(18, 'Fried Chicken',                 'Protein',   55, 2200.00, 'Crispy fried chicken piece marinated in spices'),
(19, 'Beef Suya',                     'Protein',   60, 1000.00, 'Spiced grilled beef skewers'),
(20, 'Boiled Egg (x2)',               'Protein',   80, 300.00,  'Hard boiled eggs served plain or with pepper'),
(21, 'Fried Fish',                    'Protein',   35, 1500.00, 'Whole tilapia fish seasoned and deep fried'),
(22, 'Meat Pie',                      'Snacks',    12, 1350.00, 'Baked pastry filled with seasoned minced meat'),
(23, 'Sausage Roll',                  'Snacks',    30, 600.00,  'Flaky pastry roll with seasoned sausage filling'),
(24, 'Puff Puff (6 pieces)',          'Snacks',    50, 500.00,  'Soft deep-fried dough balls lightly sweetened'),
(25, 'Chin Chin (pack)',              'Snacks',    70, 400.00,  'Crunchy fried dough snack lightly sweetened'),
(26, 'Egg Roll',                      'Snacks',    40, 700.00,  'Deep-fried pastry wrapped around a boiled egg'),
(27, 'Chapman',                       'Drinks',    25, 1200.00, 'Nigerian cocktail with Fanta Orange and Ribena'),
(28, 'Smoothie (35cl)',               'Drinks',    20, 1500.00, 'Fresh blended fruit smoothie of the day')
ON CONFLICT (id) DO NOTHING;

-- Reset sequence after manual ID inserts
SELECT setval('menu_items_id_seq', (SELECT MAX(id) FROM menu_items));
