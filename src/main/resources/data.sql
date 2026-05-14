-- ==========================================
-- INSERT INITIAL ROLES
-- ==========================================
INSERT IGNORE INTO role (id, name, description) VALUES (1, 'ADMIN', 'Administrator Role');
INSERT IGNORE INTO role (id, name, description) VALUES (2, 'USER', 'Regular User Role');
INSERT IGNORE INTO role (id, name, description) VALUES (3, 'SHOP_OWNER', 'Shop Owner Role');
INSERT IGNORE INTO role (id, name, description) VALUES (4, 'STAFF', 'Staff Role');

-- ==========================================
-- USERS (Password: 12345678)
-- ==========================================

-- ID 1-3: Admin & Owners
INSERT IGNORE INTO user (id, email, password, full_name, phone, address, active, email_verified, created_at)
VALUES (1, 'admin@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Hệ Thống Admin', '0987654321', 'Quận 1, TP.HCM', 1, 1, NOW());
INSERT IGNORE INTO user_roles (user_id, roles_id) VALUES (1, 1);
UPDATE user
SET password = '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO'
WHERE id = 1;

INSERT IGNORE INTO user (id, email, password, full_name, phone, address, active, email_verified, created_at)
VALUES (2, 'owner1@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Chủ Shop Pet Paradise', '0901111111', 'Quận 7, TP.HCM', 1, 1, NOW());
INSERT IGNORE INTO user_roles (user_id, roles_id) VALUES (2, 3);

INSERT IGNORE INTO user (id, email, password, full_name, phone, address, active, email_verified, created_at)
VALUES (3, 'owner2@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Chủ Shop Paws & Whiskers', '0902222222', 'Quận 2, TP.HCM', 1, 1, NOW());
INSERT IGNORE INTO user_roles (user_id, roles_id) VALUES (3, 3);

-- ID 4-5: Customers
INSERT IGNORE INTO user (id, email, password, full_name, phone, address, active, email_verified, created_at)
VALUES (4, 'customer1@gmail.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Nguyễn Văn Khách 1', '0911111111', 'Quận Bình Thạnh, TP.HCM', 1, 1, NOW());
INSERT IGNORE INTO user_roles (user_id, roles_id) VALUES (4, 2);

INSERT IGNORE INTO user (id, email, password, full_name, phone, address, active, email_verified, created_at)
VALUES (5, 'customer2@gmail.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Trần Thị Khách 2', '0922222222', 'Quận Thủ Đức, TP.HCM', 1, 1, NOW());
INSERT IGNORE INTO user_roles (user_id, roles_id) VALUES (5, 2);

-- ID 6-9: Staff Users
INSERT IGNORE INTO user (id, email, password, full_name, phone, address, active, email_verified, created_at)
VALUES (6, 'staff1@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Lê Nhân Viên 1 (Shop 1)', '0933333331', 'Quận 7, TP.HCM', 1, 1, NOW());
INSERT IGNORE INTO user_roles (user_id, roles_id) VALUES (6, 4);

INSERT IGNORE INTO user (id, email, password, full_name, phone, address, active, email_verified, created_at)
VALUES (7, 'staff2@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Phạm Nhân Viên 2 (Shop 1)', '0933333332', 'Quận 7, TP.HCM', 1, 1, NOW());
INSERT IGNORE INTO user_roles (user_id, roles_id) VALUES (7, 4);

INSERT IGNORE INTO user (id, email, password, full_name, phone, address, active, email_verified, created_at)
VALUES (8, 'staff3@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Bùi Nhân Viên 3 (Shop 2)', '0944444443', 'Quận 2, TP.HCM', 1, 1, NOW());
INSERT IGNORE INTO user_roles (user_id, roles_id) VALUES (8, 4);

INSERT IGNORE INTO user (id, email, password, full_name, phone, address, active, email_verified, created_at)
VALUES (9, 'staff4@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Đỗ Nhân Viên 4 (Shop 2)', '0944444444', 'Quận 2, TP.HCM', 1, 1, NOW());
INSERT IGNORE INTO user_roles (user_id, roles_id) VALUES (9, 4);

-- ==========================================
-- SHOPS
-- ==========================================
INSERT IGNORE INTO shop (id, owner_id, shop_name, email, phone, address, city, shop_type, is_verified)
VALUES (1, 2, 'Pet Paradise Spa & Clinic', 'owner1@peteye.com', '0901111111', '123 Đường số 7, Tân Phong', 'Hồ Chí Minh', 'CLINIC', 1);

INSERT IGNORE INTO shop (id, owner_id, shop_name, email, phone, address, city, shop_type, is_verified)
VALUES (2, 3, 'Paws & Whiskers Care', 'owner2@peteye.com', '0902222222', '45 Song Hành, Thảo Điền', 'Hồ Chí Minh', 'SPA', 1);

-- ==========================================
-- STAFF (Cột đúng: is_active, không có created_at)
-- ==========================================
INSERT IGNORE INTO staff (id, user_id, shop_id, full_name, role, phone, specialization, is_active)
VALUES (1, 6, 1, 'Lê Nhân Viên 1', 'GROOMER', '0933333331', 'Tắm & Cắt tỉa lông', 1);

INSERT IGNORE INTO staff (id, user_id, shop_id, full_name, role, phone, specialization, is_active)
VALUES (2, 7, 1, 'Phạm Nhân Viên 2', 'VETERINARIAN', '0933333332', 'Khám sức khỏe tổng quát', 1);

INSERT IGNORE INTO staff (id, user_id, shop_id, full_name, role, phone, specialization, is_active)
VALUES (3, 8, 2, 'Bùi Nhân Viên 3', 'GROOMER', '0944444443', 'Spa & Massage thú cưng', 1);

INSERT IGNORE INTO staff (id, user_id, shop_id, full_name, role, phone, specialization, is_active)
VALUES (4, 9, 2, 'Đỗ Nhân Viên 4', 'SUPPORT', '0944444444', 'Chăm sóc khách sạn thú cưng', 1);

-- ==========================================
-- SERVICES (Bảng đúng: pet_service, cột: duration_minutes)
-- ==========================================
INSERT IGNORE INTO pet_service (id, shop_id, service_name, description, price, duration_minutes, category, active, created_at)
VALUES (1, 1, 'Tắm & Spa Toàn Diện (S)', 'Gói tắm cao cấp cho thú cưng dưới 5kg', 250000, 60, 'Grooming', 1, NOW());

INSERT IGNORE INTO pet_service (id, shop_id, service_name, description, price, duration_minutes, category, active, created_at)
VALUES (2, 1, 'Tạo Kiểu Lông Chuyên Nghiệp', 'Cắt tỉa lông theo yêu cầu và xu hướng', 450000, 90, 'Grooming', 1, NOW());

INSERT IGNORE INTO pet_service (id, shop_id, service_name, description, price, duration_minutes, category, active, created_at)
VALUES (3, 1, 'Khám Sức Khỏe Tổng Quát', 'Xét nghiệm máu và kiểm tra lâm sàng', 600000, 45, 'Clinic', 1, NOW());

INSERT IGNORE INTO pet_service (id, shop_id, service_name, description, price, duration_minutes, category, active, created_at)
VALUES (4, 1, 'Khách Sạn Thú Cưng (Vip)', 'Phòng máy lạnh, camera 24/7', 350000, 1440, 'Hotel', 1, NOW());

INSERT IGNORE INTO pet_service (id, shop_id, service_name, description, price, duration_minutes, category, active, created_at)
VALUES (5, 2, 'Tắm Cơ Bản', 'Tắm sạch và sấy khô đơn giản', 120000, 45, 'Grooming', 1, NOW());

INSERT IGNORE INTO pet_service (id, shop_id, service_name, description, price, duration_minutes, category, active, created_at)
VALUES (6, 2, 'Vệ Sinh Tai & Cắt Móng', 'Làm sạch tai và cắt tỉa móng', 80000, 20, 'Care', 1, NOW());

INSERT IGNORE INTO pet_service (id, shop_id, service_name, description, price, duration_minutes, category, active, created_at)
VALUES (7, 2, 'Combo Grooming Tiết Kiệm', 'Tắm + vệ sinh + cắt móng nhẹ', 200000, 60, 'Grooming', 1, NOW());

INSERT IGNORE INTO pet_service (id, shop_id, service_name, description, price, duration_minutes, category, active, created_at)
VALUES (8, 2, 'Gửi Thú Cưng Trong Ngày', 'Dịch vụ daycare cơ bản', 100000, 480, 'Hotel', 1, NOW());

INSERT IGNORE INTO pet_service (id, shop_id, service_name, description, price, duration_minutes, category, active, created_at)
VALUES (9, 2, 'Massage Thú Cưng', 'Massage thư giãn 30 phút', 150000, 30, 'Care', 1, NOW());

-- ==========================================
-- WALLETS
-- ==========================================
INSERT IGNORE INTO shop_wallet (id, shop_id, available_balance, frozen_balance, total_earned, total_withdrawn, updated_at)
VALUES (1, 1, 0, 0, 0, 0, NOW());

INSERT IGNORE INTO shop_wallet (id, shop_id, available_balance, frozen_balance, total_earned, total_withdrawn, updated_at)
VALUES (2, 2, 0, 0, 0, 0, NOW());
