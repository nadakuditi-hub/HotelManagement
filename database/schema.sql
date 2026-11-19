-- Hotel Management System: MySQL schema + sample data
-- Run: mysql -u root -p < database/schema.sql
-- (Spring Boot also auto-creates/updates tables via JPA ddl-auto=update; this file is for reference/ER diagram.)

CREATE DATABASE IF NOT EXISTS hotel_db;
USE hotel_db;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(120) NOT NULL UNIQUE,
  phone VARCHAR(20),
  password VARCHAR(255) NOT NULL,
  role ENUM('CUSTOMER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rooms (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_number VARCHAR(10) NOT NULL UNIQUE,
  type ENUM('SINGLE','DOUBLE','DELUXE','SUITE') NOT NULL,
  price_per_night DECIMAL(10,2) NOT NULL,
  capacity INT NOT NULL,
  description VARCHAR(500),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  image_url VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS bookings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  check_in DATE NOT NULL,
  check_out DATE NOT NULL,
  guests INT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  status ENUM('CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (room_id) REFERENCES rooms(id)
);

CREATE TABLE IF NOT EXISTS payments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  booking_id BIGINT NOT NULL UNIQUE,
  amount DECIMAL(10,2) NOT NULL,
  method ENUM('CARD','UPI','CASH') NOT NULL,
  status ENUM('PAID','REFUNDED') NOT NULL DEFAULT 'PAID',
  invoice_number VARCHAR(30) NOT NULL UNIQUE,
  paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE TABLE IF NOT EXISTS reviews (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  booking_id BIGINT NOT NULL UNIQUE,          -- one review per booking
  user_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  rating INT NOT NULL,                        -- 1 to 5
  comment VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (booking_id) REFERENCES bookings(id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (room_id) REFERENCES rooms(id)
);

-- Sample rooms (the backend also seeds an admin user on first run:
--   admin@hotel.com / admin123)
INSERT IGNORE INTO rooms (room_number, type, price_per_night, capacity, description) VALUES
('101','SINGLE',1500,1,'Cozy single room with city view'),
('102','SINGLE',1500,1,'Single room with work desk'),
('201','DOUBLE',2500,2,'Double bed, balcony, free breakfast'),
('202','DOUBLE',2500,2,'Twin beds, garden view'),
('301','DELUXE',4000,3,'Spacious deluxe room with bathtub'),
('302','DELUXE',4200,3,'Deluxe room, king bed, lounge area'),
('401','SUITE',7500,4,'Luxury suite with living room and jacuzzi');

-- ============ Round 2 tables (created automatically by Spring Boot; listed here for reference) ============
CREATE TABLE IF NOT EXISTS add_ons (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(60) NOT NULL,
  description VARCHAR(200),
  price DECIMAL(10,2) NOT NULL,
  per_night BOOLEAN NOT NULL DEFAULT FALSE,
  active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS booking_addons (
  booking_id BIGINT NOT NULL,
  name VARCHAR(60) NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE TABLE IF NOT EXISTS coupons (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(30) NOT NULL UNIQUE,
  description VARCHAR(200),
  type ENUM('PERCENT','FLAT') NOT NULL,
  discount_value DECIMAL(10,2) NOT NULL,
  min_amount DECIMAL(10,2),
  max_discount DECIMAL(10,2),
  valid_from DATE,
  valid_to DATE,
  usage_limit INT,
  used_count INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS staff (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  role ENUM('RECEPTIONIST','HOUSEKEEPING','MAINTENANCE','CHEF','SECURITY','MANAGER') NOT NULL,
  phone VARCHAR(20),
  email VARCHAR(120),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS housekeeping_tasks (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_id BIGINT NOT NULL,
  type ENUM('CLEANING','MAINTENANCE') NOT NULL,
  status ENUM('OPEN','IN_PROGRESS','DONE') NOT NULL DEFAULT 'OPEN',
  staff_id BIGINT,
  notes VARCHAR(300),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP NULL,
  FOREIGN KEY (room_id) REFERENCES rooms(id),
  FOREIGN KEY (staff_id) REFERENCES staff(id)
);

CREATE TABLE IF NOT EXISTS menu_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  description VARCHAR(200),
  category ENUM('BREAKFAST','STARTER','MAIN_COURSE','DESSERT','BEVERAGE') NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  veg BOOLEAN NOT NULL DEFAULT TRUE,
  image_url VARCHAR(500),
  available BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS food_orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  booking_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  status ENUM('PLACED','PREPARING','DELIVERED','CANCELLED') NOT NULL DEFAULT 'PLACED',
  paid BOOLEAN NOT NULL DEFAULT FALSE,
  notes VARCHAR(200),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE TABLE IF NOT EXISTS food_order_items (
  order_id BIGINT NOT NULL,
  name VARCHAR(80) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  quantity INT NOT NULL,
  FOREIGN KEY (order_id) REFERENCES food_orders(id)
);
-- New columns on bookings (added automatically): room_amount, addons_amount, discount_amount, coupon_code
