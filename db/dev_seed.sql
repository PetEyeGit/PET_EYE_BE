-- ============================================================
-- PETEYE — DEV SEED (tối giản để test luồng booking)
-- ============================================================
-- Tài khoản:
--   Admin      : admin@peteye.com     / 12345678
--   Shop Owner : shopdev@peteye.com   / 12345678
--   User       : userdev@gmail.com    / 12345678
--   Staff      : staffdev@peteye.com  / 12345678  (trong bảng staff, không đăng nhập)
--
-- Shop: "Dev Pet Spa" — đã verified, mode MANUAL
-- Dịch vụ:
--   ID 1 — Tắm & Sấy - Chó (< 5kg) : 150,000đ  / 60 phút
--   ID 2 — Tắm & Sấy - Chó (5-10kg): 200,000đ  / 60 phút
--   ID 3 — Cắt tỉa lông - Chó (< 5kg) : 250,000đ / 90 phút
--   ... và các dịch vụ khác cho Mèo và theo số kg
-- Pet: "Mochi" (Poodle, đực) — thuộc userdev
-- Ví shop: 0đ (chưa có giao dịch nào)
-- ============================================================

USE PET_EYE;

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES    = 0;

-- ============================================================
-- TRUNCATE (xóa sạch toàn bộ data cũ)
-- ============================================================
TRUNCATE TABLE withdrawal_request;
TRUNCATE TABLE shop_wallet;
TRUNCATE TABLE transaction;
TRUNCATE TABLE review;
TRUNCATE TABLE payment;
TRUNCATE TABLE booking;
TRUNCATE TABLE booking_services;
TRUNCATE TABLE boarding_detail;
TRUNCATE TABLE cage;
TRUNCATE TABLE camera;
TRUNCATE TABLE pet_medical_record;
TRUNCATE TABLE pet_vaccination;
TRUNCATE TABLE pet_reminder;
TRUNCATE TABLE pet_image;
TRUNCATE TABLE pet;
TRUNCATE TABLE staff_certificate;
TRUNCATE TABLE staff;
TRUNCATE TABLE pet_service;
TRUNCATE TABLE shop;
TRUNCATE TABLE notification;
TRUNCATE TABLE invalidated_token;
TRUNCATE TABLE user_roles;
TRUNCATE TABLE role;
TRUNCATE TABLE user;

-- Không gán lại FOREIGN_KEY_CHECKS = 1 ở đây, sẽ để ở cuối file

-- ============================================================
-- ROLES
-- ============================================================
INSERT INTO role (id, name, description) VALUES
(1, 'ADMIN',      'Quản trị viên'),
(2, 'USER',       'Khách hàng'),
(3, 'SHOP_OWNER', 'Chủ cửa hàng'),
(4, 'STAFF',      'Nhân viên');

-- ============================================================
-- USERS
-- Mật khẩu tất cả: 12345678
-- BCrypt hash của "12345678":
--   $2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO
-- ============================================================
INSERT INTO user (id, email, password, full_name, phone, address, created_at, email_verified, active) VALUES
-- ID 1: Admin
(1, 'admin@peteye.com',
 '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO',
 'Dev Admin', '0900000001', 'Quận 1, TP.HCM', NOW(), true, true),

-- ID 2: Shop Owner (dùng để đăng nhập quản lý shop)
(2, 'shopdev@peteye.com',
 '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO',
 'Dev Shop Owner', '0900000002', 'Quận 5, TP.HCM', NOW(), true, true),

-- ID 3: User (dùng để đặt lịch)
(3, 'userdev@gmail.com',
 '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO',
 'Dev User', '0911000001', 'Quận 3, TP.HCM', NOW(), true, true),

-- ID 4: Staff user account (chỉ để liên kết với bảng staff, không cần đăng nhập)
(4, 'staffdev@peteye.com',
 '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO',
 'Dev Staff', '0922000001', 'TP.HCM', NOW(), true, true);

-- Gán roles
INSERT INTO user_roles (user_id, roles_id) VALUES
(1, 1),  -- admin     → ADMIN
(2, 3),  -- shopdev   → SHOP_OWNER
(3, 2),  -- userdev   → USER
(4, 4);  -- staffdev  → STAFF

-- ============================================================
-- SHOP
-- ============================================================
INSERT INTO shop (id, owner_id, shop_name, shop_type, email, phone,
                  address, city, description, license_number,
                  is_verified, rating_avg, open_time, close_time,
                  working_days, assignment_mode, late_grace_period, use_banner_in_gallery, show_cover_in_gallery, status) VALUES
(1, 2,
 'Dev Pet Spa',
 'SPA',
 'shopdev@peteye.com',
 '0281000001',
 '123 Nguyễn Trãi, Quận 5',
 'TP.HCM',
 'Shop DEV dùng để test luồng booking. Đầy đủ dịch vụ spa & grooming.',
 'DEV-001',
 true,       -- is_verified = true (bắt buộc để đặt lịch)
 5.0,
 '08:00',
 '20:00',
 'Mon,Tue,Wed,Thu,Fri,Sat,Sun',
 'MANUAL', 15, true, true, 'APPROVED');  -- Thêm use_banner_in_gallery, show_cover_in_gallery và status

-- ============================================================
-- SERVICES (Chia theo loại thú cưng và số kg)
-- ============================================================
INSERT INTO pet_service (id, shop_id, service_name, category, pet_type, price, duration_minutes,
                         description, active, camera_enabled, created_at) VALUES
-- Dịch vụ cho Chó
(1, 1, 'Tắm & Sấy - Chó (< 5kg)', 'SPA', 'DOG', 150000, 60, 'Tắm sấy cơ bản cho chó nhỏ dưới 5kg', true, false, NOW()),
(2, 1, 'Tắm & Sấy - Chó (5 - 10kg)', 'SPA', 'DOG', 200000, 60, 'Tắm sấy cơ bản cho chó vừa từ 5-10kg', true, false, NOW()),
(3, 1, 'Tắm & Sấy - Chó (> 10kg)', 'SPA', 'DOG', 250000, 60, 'Tắm sấy cơ bản cho chó lớn trên 10kg', true, false, NOW()),
(4, 1, 'Cắt tỉa lông - Chó (< 5kg)', 'GROOMING', 'DOG', 250000, 90, 'Cắt tỉa lông cho chó nhỏ dưới 5kg', true, false, NOW()),
(5, 1, 'Cắt tỉa lông - Chó (5 - 10kg)', 'GROOMING', 'DOG', 300000, 90, 'Cắt tỉa lông cho chó vừa từ 5-10kg', true, false, NOW()),
(6, 1, 'Cắt tỉa lông - Chó (> 10kg)', 'GROOMING', 'DOG', 350000, 90, 'Cắt tỉa lông cho chó lớn trên 10kg', true, false, NOW()),

-- Dịch vụ cho Mèo
(7, 1, 'Tắm & Sấy - Mèo (< 5kg)', 'SPA', 'CAT', 150000, 60, 'Tắm sấy cơ bản cho mèo nhỏ dưới 5kg', true, false, NOW()),
(8, 1, 'Tắm & Sấy - Mèo (5 - 10kg)', 'SPA', 'CAT', 200000, 60, 'Tắm sấy cơ bản cho mèo vừa từ 5-10kg', true, false, NOW()),
(9, 1, 'Cắt tỉa lông - Mèo (< 5kg)', 'GROOMING', 'CAT', 250000, 90, 'Cắt tỉa lông cho mèo nhỏ dưới 5kg', true, false, NOW()),
(10, 1, 'Cắt tỉa lông - Mèo (5 - 10kg)', 'GROOMING', 'CAT', 300000, 90, 'Cắt tỉa lông cho mèo vừa từ 5-10kg', true, false, NOW()),

-- Lưu trú
(11, 1, 'Lưu trú thú cưng kèm Camera', 'BOARDING', 'DOG', 300000, 1440, 'Dịch vụ lưu trú cao cấp có camera giám sát 24/7', true, true, NOW());

-- ============================================================
-- STAFF (1 nhân viên)
-- ============================================================
INSERT INTO staff (id, shop_id, user_id, full_name, role, phone, specialization, is_active) VALUES
(1, 1, 4,
 'Dev Staff',
 'GROOMER',
 '0922000001',
 'Cắt tỉa lông, spa, tắm dưỡng',
 true);

-- ============================================================
-- PET (1 pet thuộc userdev)
-- ============================================================
INSERT INTO pet (id, owner_id, name, species, breed, gender, color,
                 sterilized, weight, dob, health_note, is_active) VALUES
(1, 3,
 'Mochi',
 'Chó',
 'Poodle',
 'Đực',
 'Trắng kem',
 false,
 3.5,
 '2022-06-15',
 'Khỏe mạnh, không dị ứng',
 true);

-- ============================================================
-- SHOP WALLET (ví trống — chưa có giao dịch nào)
-- ============================================================
INSERT INTO shop_wallet (id, shop_id, frozen_balance, available_balance, total_earned, total_withdrawn, updated_at) VALUES
(1, 1, 0.00, 0.00, 0.00, 0.00, NOW());

-- ============================================================
-- DONE
-- ============================================================
-- Kiểm tra nhanh:
--   SELECT * FROM user;
--   SELECT * FROM shop;
--   SELECT * FROM pet_service;
--   SELECT * FROM staff;
--   SELECT * FROM pet;
--   SELECT * FROM shop_wallet;
-- ============================================================
-- ============================================================
-- BỔ SUNG THÊM STAFF 3 (Dev Staff 3)
-- ============================================================

-- Bước 1: Khởi tạo USER (ID = 6)
INSERT INTO user (id, email, password, full_name, phone, address, created_at, email_verified, active) VALUES
    (6, 'staffdev3@peteye.com',
     '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', -- Mật khẩu vẫn là 12345678
     'Dev Staff 3', '0922000003', 'TP.HCM', NOW(), true, true);

-- Bước 2: Gán ROLE STAFF (roles_id = 4) cho user_id = 6
INSERT INTO user_roles (user_id, roles_id) VALUES
    (6, 4);  -- staffdev3 → STAFF

-- Bước 3: Tạo thông tin chi tiết trong bảng STAFF (ID = 3, liên kết với shop_id = 1)
INSERT INTO staff (id, shop_id, user_id, full_name, role, phone, specialization, is_active) VALUES
    (3, 1, 6,
     'Dev Staff 3',
     'GROOMER',
     '0922000003',
     'Điều trị da liễu, cắt tỉa chuyên sâu, tạo kiểu',
     true);

-- ============================================================
-- CAGES & CAMERAS (Lưu trú sử dụng camera)
-- ============================================================
INSERT INTO cage (id, shop_id, cage_code, type, is_available) VALUES
(1, 1, 'C101', 'NORMAL', true),
(2, 1, 'C102', 'NORMAL', true),
(3, 1, 'VIP01', 'VIP', true);

INSERT INTO camera (id, cage_id, model_type, stream_url, access_token, status) VALUES
(1, 3, 'KBONE-H21W', 'rtsp://admin:123456@192.168.1.100:554/live', 'token123', 'ONLINE'),
(2, 1, 'EZVIZ-C6N', 'rtsp://admin:123456@192.168.1.101:554/live', 'token456', 'ONLINE');

-- ============================================================
-- KHÔNG CÓ DỮ LIỆU BOOKING
-- (Chỉ có user, shop, services, staff, pet, cages & cameras)
-- ============================================================


SET FOREIGN_KEY_CHECKS = 1;