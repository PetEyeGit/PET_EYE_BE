-- ==========================================
-- INSERT INITIAL ROLES (Use IGNORE to avoid errors if already exists)
-- ==========================================
INSERT IGNORE INTO role (id, name, description) VALUES (1, 'ADMIN', 'Administrator Role');
INSERT IGNORE INTO role (id, name, description) VALUES (2, 'USER', 'Regular User Role');
INSERT IGNORE INTO role (id, name, description) VALUES (3, 'SHOP_OWNER', 'Shop Owner Role');

-- ==========================================
-- INSERT ADMIN USER
-- Email: admin@peteye.com
-- Password: 12345678 (BCrypt encoded)
-- ==========================================
INSERT IGNORE INTO user (id, email, password, full_name, phone, address, created_at)
VALUES (1, 'admin@peteye.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'System Admin', '0987654321', 'District 1, HCM City', NOW());

-- Link Admin user (ID=1) to Admin role (ID=1)
INSERT IGNORE INTO user_roles (user_id, roles_id) VALUES (1, 1);
