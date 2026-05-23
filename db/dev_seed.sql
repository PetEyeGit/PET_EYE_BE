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
--   ID 1 — Tắm & Sấy cơ bản     : 150,000đ  / 60 phút
--   ID 2 — Cắt tỉa lông          : 250,000đ  / 90 phút
--   ID 3 — Gói Spa thư giãn      : 450,000đ  / 120 phút
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

SET FOREIGN_KEY_CHECKS = 1;

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
                  working_days, assignment_mode) VALUES
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
 'MANUAL');  -- MANUAL: shop owner tự assign staff

-- ============================================================
-- SERVICES (3 dịch vụ)
-- ============================================================
INSERT INTO pet_service (id, shop_id, service_name, category, price, duration_minutes,
                         description, active, camera_enabled, created_at) VALUES
(1, 1,
 'Tắm & Sấy cơ bản',
 'SPA',
 150000,
 60,
 'Tắm sạch bằng sữa tắm chuyên dụng, sấy khô hoàn toàn, vệ sinh tai và cắt móng.',
 true, false, NOW()),

(2, 1,
 'Cắt tỉa lông toàn thân',
 'GROOMING',
 250000,
 90,
 'Cắt tỉa lông theo yêu cầu, tạo kiểu chuyên nghiệp. Bao gồm tắm và sấy.',
 true, false, NOW()),

(3, 1,
 'Gói Spa thư giãn',
 'SPA',
 450000,
 120,
 'Tắm thơm, massage toàn thân, dưỡng lông mềm mượt, cắt móng, vệ sinh tai, xịt nước hoa.',
 true, false, NOW());

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
-- BOOKINGS - Các đơn hàng với các trạng thái khác nhau
-- ============================================================

-- Booking 1: UPCOMING (Sắp diễn ra) - CONFIRMED status
-- Ngày mai, 10:00 AM
INSERT INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, created_at) VALUES
(1, 3, 1, 1, 1, 1,
 DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 10 HOUR,
 'CONFIRMED',
 'Tắm & sấy cơ bản cho Mochi - Sắp diễn ra',
 NOW());

-- Booking 2: UPCOMING (Sắp diễn ra) - CONFIRMED status
-- Ngày kia, 14:00 PM
INSERT INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, created_at) VALUES
(2, 3, 1, 2, 1, 3,
 DATE_ADD(CURDATE(), INTERVAL 2 DAY) + INTERVAL 14 HOUR,
 'CONFIRMED',
 'Cắt tỉa lông toàn thân - Sắp diễn ra',
 NOW());

-- Booking 3: ONGOING (Đang diễn ra) - IN_PROGRESS status
-- Hôm nay, 11:00 AM (giả sử đang trong giờ làm việc)
INSERT INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, created_at) VALUES
(3, 3, 1, 3, 1, 1,
 CURDATE() + INTERVAL 11 HOUR,
 'IN_PROGRESS',
 'Gói Spa thư giãn - Đang diễn ra',
 NOW());

-- Booking 4: COMPLETED (Hoàn thành) - COMPLETED status
-- Hôm qua, 09:00 AM
INSERT INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, created_at) VALUES
(4, 3, 1, 1, 1, 3,
 DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 9 HOUR,
 'COMPLETED',
 'Tắm & sấy cơ bản - Đã hoàn thành',
 DATE_SUB(NOW(), INTERVAL 1 DAY));

-- Booking 5: COMPLETED (Hoàn thành) - COMPLETED status
-- 2 ngày trước, 15:00 PM
INSERT INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, created_at) VALUES
(5, 3, 1, 2, 1, 1,
 DATE_SUB(CURDATE(), INTERVAL 2 DAY) + INTERVAL 15 HOUR,
 'COMPLETED',
 'Cắt tỉa lông - Đã hoàn thành',
 DATE_SUB(NOW(), INTERVAL 2 DAY));

-- Booking 6: CANCELLED (Đã hủy) - CANCELLED status
-- 3 ngày trước, 10:00 AM
INSERT INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, created_at) VALUES
(6, 3, 1, 3, 1, 3,
 DATE_SUB(CURDATE(), INTERVAL 3 DAY) + INTERVAL 10 HOUR,
 'CANCELLED',
 'Gói Spa thư giãn - Đã hủy',
 DATE_SUB(NOW(), INTERVAL 3 DAY));

-- Booking 7: WAITING_SHOP_APPROVAL (Chờ phê duyệt) - WAITING_SHOP_APPROVAL status
-- Ngày mai, 16:00 PM
INSERT INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, created_at) VALUES
(7, 3, 1, 1, 1, NULL,
 DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 16 HOUR,
 'WAITING_SHOP_APPROVAL',
 'Tắm & sấy cơ bản - Chờ phê duyệt từ shop',
 NOW());