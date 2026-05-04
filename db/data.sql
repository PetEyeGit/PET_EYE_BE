-- ==========================================
-- TRUNCATE ALL TABLES
-- ==========================================
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE user_roles;
TRUNCATE TABLE role;
TRUNCATE TABLE pet;
TRUNCATE TABLE shop;
TRUNCATE TABLE service;
TRUNCATE TABLE staff;
TRUNCATE TABLE booking;
TRUNCATE TABLE booking_history;
TRUNCATE TABLE transaction;
TRUNCATE TABLE cage;
TRUNCATE TABLE camera;
TRUNCATE TABLE boarding_detail;
TRUNCATE TABLE pet_medical_record;
TRUNCATE TABLE notification;
TRUNCATE TABLE review;
TRUNCATE TABLE payment;
TRUNCATE TABLE user;
TRUNCATE TABLE invalidated_token;

SET FOREIGN_KEY_CHECKS = 1;

-- ==========================================
-- INSERT INITIAL ROLES
-- ==========================================
INSERT INTO role (name, description) VALUES ('ADMIN', 'Administrator Role');
INSERT INTO role (name, description) VALUES ('USER', 'Regular User Role');
INSERT INTO role (name, description) VALUES ('SHOP_OWNER', 'Shop Owner Role');

-- ==========================================
-- INSERT ADMIN USER
-- Email: admin@peteye.com
-- Password: 12345678 (BCrypt encoded)
-- ==========================================
INSERT INTO user (email, password, full_name, phone, address, created_at, email_verified, active)
VALUES ('admin@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'System Admin', '0987654321', 'District 1, HCM City', NOW(), true, true);

-- Link Admin user (ID=1) to Admin role (ID=1)
INSERT INTO user_roles (user_id, roles_id) VALUES (1, 1);
