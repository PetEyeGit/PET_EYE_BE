-- ==========================================
-- INSERT INITIAL ROLES (Use IGNORE to avoid errors if already exists)
-- ==========================================
INSERT IGNORE INTO role (id, name, description) VALUES (1, 'ADMIN', 'Administrator Role');
INSERT IGNORE INTO role (id, name, description) VALUES (2, 'USER', 'Regular User Role');
INSERT IGNORE INTO role (id, name, description) VALUES (3, 'SHOP_OWNER', 'Shop Owner Role');
INSERT IGNORE INTO role (id, name, description) VALUES (4, 'STAFF', 'Staff Role');

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

INSERT IGNORE INTO user_roles (user_id, roles_id) VALUES (3, 4); -- Role STAFF (ID=4)

INSERT IGNORE INTO staff (id, user_id, shop_id, full_name, phone, is_active)
VALUES (1, 3, 1, 'Staff Test', '0999888777', 1);

-- ==========================================
-- THÊM 2 USER KHÁCH HÀNG (Pass: 12345678)
-- ==========================================
INSERT IGNORE INTO user (id, email, password, full_name, phone, address, active, email_verified, created_at)
VALUES (5, 'user1@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Nguyễn Văn A', '0911222333', 'Quận 1, HCM', 1, 1, NOW());

INSERT IGNORE INTO user (id, email, password, full_name, phone, address, active, email_verified, created_at)
VALUES (6, 'user2@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Trần Thị B', '0944555666', 'Quận 3, HCM', 1, 1, NOW());

-- Gán Role USER (ID=2) cho 2 khách hàng này
INSERT IGNORE INTO user_roles (user_id, roles_id) VALUES (5, 2);
INSERT IGNORE INTO user_roles (user_id, roles_id) VALUES (6, 2);

-- ==========================================
-- THÊM THÚ CƯNG CHO KHÁCH HÀNG
-- ==========================================
INSERT IGNORE INTO pet (id, owner_id, name, species, breed, gender, weight, dob, is_active)
VALUES (1, 5, 'Lu', 'Dog', 'Golden Retriever', 'Male', 15.5, '2023-01-01', 1);

INSERT IGNORE INTO pet (id, owner_id, name, species, breed, gender, weight, dob, is_active)
VALUES (2, 6, 'Miu', 'Cat', 'British Shorthair', 'Female', 4.2, '2023-05-10', 1);

-- ==========================================
-- THÊM DỊCH VỤ CHO SHOP (Shop ID = 1)
-- ==========================================
INSERT IGNORE INTO pet_service (id, shop_id, service_name, category, price, duration_minutes, active, created_at)
VALUES (1, 1, 'Tắm sấy trọn gói', 'SPA', 100000, 60, 1, NOW());

INSERT IGNORE INTO pet_service (id, shop_id, service_name, category, price, duration_minutes, active, created_at)
VALUES (2, 1, 'Cắt tỉa lông nghệ thuật', 'GROOMING', 150000, 90, 1, NOW());

-- ==========================================
-- THÊM ĐƠN HÀNG MẪU (BOOKING)
-- ==========================================
-- Đơn hàng 1: Nguyễn Văn A đặt Tắm chó Lu (Đã xác nhận)
INSERT IGNORE INTO booking (id, user_id, shop_id, service_id, pet_id, appointment_datetime, status, note, created_at)
VALUES (1, 5, 1, 1, 1, DATE_ADD(NOW(), INTERVAL 1 DAY), 'CONFIRMED', 'Nhớ tắm kỹ cho bé', NOW());

-- Đơn hàng 2: Trần Thị B đặt Cắt tỉa cho Mèo Miu (Đã hoàn thành)
INSERT IGNORE INTO booking (id, user_id, shop_id, service_id, pet_id, appointment_datetime, status, created_at)
VALUES (2, 6, 1, 2, 2, DATE_SUB(NOW(), INTERVAL 2 DAY), 'COMPLETED', NOW());

-- Đơn hàng 3: Nguyễn Văn A đặt Cắt tỉa (Đang chờ thanh toán)
INSERT IGNORE INTO booking (id, user_id, shop_id, service_id, pet_id, appointment_datetime, status, payos_order_code, created_at)
VALUES (3, 5, 1, 2, 1, DATE_ADD(NOW(), INTERVAL 2 DAY), 'PENDING_PAYMENT', 123456789, NOW());
