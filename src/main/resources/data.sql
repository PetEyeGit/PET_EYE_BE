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


-- 1. Tạo một User với vai trò Chủ Shop (Email: owner@peteye.com / Pass: 12345678)
INSERT IGNORE INTO user (id, email, password, full_name, phone, address, active, email_verified, created_at)
VALUES (2, 'owner@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Shop Owner Test', '0123456789', 'District 7, HCM City', 1, 1, NOW());

-- 2. Gán Role SHOP_OWNER (ID=3) cho User này (ID=2)
INSERT IGNORE INTO user_roles (user_id, roles_id) VALUES (2, 3);

-- 3. Tạo dữ liệu Shop đã được xác thực (is_verified = 1) cho User trên
-- Lưu ý: Tên các cột phải khớp với mapping của Hibernate (thường là snake_case)
INSERT IGNORE INTO shop (id, owner_id, shop_name, shop_type, email, phone, address, city, is_verified, rating_avg, assignment_mode)
VALUES (1, 2, 'Pet Eye Test Shop', 'CLINIC', 'owner@peteye.com', '0123456789', '123 Test St', 'HCM', 1, 5.0, 'MANUAL');

-- 4. Tạo thêm một nhân viên mẫu cho Shop này để test phân việc (Email: staff@peteye.com / Pass: 12345678)
INSERT IGNORE INTO user (id, email, password, full_name, phone, address, active, email_verified, created_at)
VALUES (3, 'staff@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Staff Test', '0999888777', 'District 7, HCM City', 1, 1, NOW());

INSERT IGNORE INTO user_roles (user_id, roles_id) VALUES (3, 4); -- Role USER (nhân viên thường là user có role staff riêng)

INSERT IGNORE INTO staff (id, user_id, shop_id, full_name, phone, is_active)
VALUES (1, 3, 1, 'Staff Test', '0999888777', 1);
