
-- ============================================================
-- PETEYE — SEED DATA
-- Chạy: USE PET_EYE; source seed_data.sql;
--
-- Cách đăng nhập:
--   Shop Owner : dùng email owner (trong bảng user, role SHOP_OWNER)
--   Admin      : admin@peteye.com / 12345678
--   User       : user1@gmail.com  / 12345678
--   Staff      : staff1@peteye.com / 12345678
--
-- Mật khẩu tất cả: 12345678
-- ============================================================

USE PET_EYE;

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES    = 0;

-- ============================================================
-- TRUNCATE
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
-- ┌─────────────────────────────────────────────────────────┐
-- │  ID 1       : admin@peteye.com        → ADMIN           │
-- │  ID 2-6     : shop1@peteye.com..5     → SHOP_OWNER      │
-- │               (đây là tài khoản đăng nhập của shop)     │
-- │  ID 7-16    : user1@gmail.com..10     → USER            │
-- │  ID 17-26   : staff1@peteye.com..10   → STAFF           │
-- └─────────────────────────────────────────────────────────┘
-- ============================================================

-- Admin
INSERT INTO user (id, email, password, full_name, phone, address, created_at, email_verified, active) VALUES
(1, 'admin@peteye.com',
 '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO',
 'Admin', '0900000001', 'Quận 1, TP.HCM', NOW(), true, true);

-- Shop Owner accounts (email này dùng để đăng nhập vào giao diện Shop)
INSERT INTO user (id, email, password, full_name, phone, address, created_at, email_verified, active) VALUES
(2, 'shop1@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Shop 1 Owner', '0900000002', 'Quận 5, TP.HCM',          NOW(), true, true),
(3, 'shop2@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Shop 2 Owner', '0900000003', 'Quận 3, TP.HCM',          NOW(), true, true),
(4, 'shop3@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Shop 3 Owner', '0900000004', 'Quận Bình Thạnh, TP.HCM', NOW(), true, true),
(5, 'shop4@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Shop 4 Owner', '0900000005', 'Quận Phú Nhuận, TP.HCM',  NOW(), true, true),
(6, 'shop5@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Shop 5 Owner', '0900000006', 'Quận 1, TP.HCM',          NOW(), true, true);

-- Regular users
INSERT INTO user (id, email, password, full_name, phone, address, created_at, email_verified, active) VALUES
(7,  'user1@gmail.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'User 1',  '0911000001', 'Quận 1, TP.HCM',  NOW(), true, true),
(8,  'user2@gmail.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'User 2',  '0911000002', 'Quận 3, TP.HCM',  NOW(), true, true),
(9,  'user3@gmail.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'User 3',  '0911000003', 'Quận 5, TP.HCM',  NOW(), true, true),
(10, 'user4@gmail.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'User 4',  '0911000004', 'Quận 7, TP.HCM',  NOW(), true, true),
(11, 'user5@gmail.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'User 5',  '0911000005', 'Quận 10, TP.HCM', NOW(), true, true),
(12, 'user6@gmail.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'User 6',  '0911000006', 'Quận 11, TP.HCM', NOW(), true, true),
(13, 'user7@gmail.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'User 7',  '0911000007', 'Quận 12, TP.HCM', NOW(), true, true),
(14, 'user8@gmail.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'User 8',  '0911000008', 'Bình Thạnh',      NOW(), true, true),
(15, 'user9@gmail.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'User 9',  '0911000009', 'Phú Nhuận',       NOW(), true, true),
(16, 'user10@gmail.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'User 10', '0911000010', 'Tân Bình',        NOW(), true, true);

-- Staff accounts
INSERT INTO user (id, email, password, full_name, phone, address, created_at, email_verified, active) VALUES
(17, 'staff1@peteye.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Staff 1',  '0922000001', 'TP.HCM', NOW(), true, true),
(18, 'staff2@peteye.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Staff 2',  '0922000002', 'TP.HCM', NOW(), true, true),
(19, 'staff3@peteye.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Staff 3',  '0922000003', 'TP.HCM', NOW(), true, true),
(20, 'staff4@peteye.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Staff 4',  '0922000004', 'TP.HCM', NOW(), true, true),
(21, 'staff5@peteye.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Staff 5',  '0922000005', 'TP.HCM', NOW(), true, true),
(22, 'staff6@peteye.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Staff 6',  '0922000006', 'TP.HCM', NOW(), true, true),
(23, 'staff7@peteye.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Staff 7',  '0922000007', 'TP.HCM', NOW(), true, true),
(24, 'staff8@peteye.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Staff 8',  '0922000008', 'TP.HCM', NOW(), true, true),
(25, 'staff9@peteye.com',  '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Staff 9',  '0922000009', 'TP.HCM', NOW(), true, true),
(26, 'staff10@peteye.com', '$2a$10$9uurETzMx/LPgDYIodiRm.65/zfb7aJK5asJc.6wC1Nn26QfzNRcO', 'Staff 10', '0922000010', 'TP.HCM', NOW(), true, true);

-- User Roles
INSERT INTO user_roles (user_id, roles_id) VALUES
(1,  1),                                          -- admin
(2,  3),(3,  3),(4,  3),(5,  3),(6,  3),          -- shop owners
(7,  2),(8,  2),(9,  2),(10, 2),(11, 2),           -- users
(12, 2),(13, 2),(14, 2),(15, 2),(16, 2),           -- users
(17, 4),(18, 4),(19, 4),(20, 4),(21, 4),           -- staff
(22, 4),(23, 4),(24, 4),(25, 4),(26, 4);           -- staff

-- ============================================================
-- SHOPS
-- owner_id trỏ đến user có role SHOP_OWNER
-- Đăng nhập shop bằng: shop1@peteye.com / 12345678
-- ============================================================
INSERT INTO shop (id, owner_id, shop_name, shop_type, email, phone,
                  address, city, description, license_number,
                  is_verified, rating_avg, open_time, close_time,
                  working_days, assignment_mode) VALUES
(1, 2, 'Shop 1', 'SPA',
   'shop1@peteye.com', '0281000001',
   '123 Nguyễn Trãi, Quận 5', 'TP.HCM',
   'Shop 1 chuyên dịch vụ Spa & Grooming cho thú cưng tại Quận 5. Đội ngũ nhân viên tận tình, sản phẩm cao cấp.',
   'SP-001', true, 4.7, '08:00', '20:00', 'Mon,Tue,Wed,Thu,Fri,Sat,Sun', 'MANUAL'),

(2, 3, 'Shop 2', 'CLINIC',
   'shop2@peteye.com', '0281000002',
   '45 Lê Văn Sỹ, Quận 3', 'TP.HCM',
   'Shop 2 là phòng khám thú y uy tín tại Quận 3. Bác sĩ 10 năm kinh nghiệm, trang thiết bị hiện đại.',
   'CL-002', true, 4.8, '07:00', '21:00', 'Mon,Tue,Wed,Thu,Fri,Sat,Sun', 'AUTO'),

(3, 4, 'Shop 3', 'BOARDING',
   'shop3@peteye.com', '0281000003',
   '78 Đinh Tiên Hoàng, Quận Bình Thạnh', 'TP.HCM',
   'Shop 3 cung cấp dịch vụ lưu trú & khách sạn thú cưng tại Bình Thạnh. Camera 24/7, phòng riêng thoáng mát.',
   'BD-003', true, 4.6, '06:00', '22:00', 'Mon,Tue,Wed,Thu,Fri,Sat,Sun', 'OPEN_POOL'),

(4, 5, 'Shop 4', 'SPA',
   'shop4@peteye.com', '0281000004',
   '200 Hoàng Văn Thụ, Quận Phú Nhuận', 'TP.HCM',
   'Shop 4 chuyên làm đẹp và grooming cao cấp tại Phú Nhuận. Nhuộm lông nghệ thuật, spa thư giãn.',
   'SP-004', true, 4.5, '09:00', '19:00', 'Mon,Tue,Wed,Thu,Fri,Sat', 'MANUAL'),

(5, 6, 'Shop 5', 'MIXED',
   'shop5@peteye.com', '0281000005',
   '15 Nguyễn Đình Chiểu, Quận 1', 'TP.HCM',
   'Shop 5 là trung tâm chăm sóc thú cưng cao cấp tổng hợp tại Quận 1. Đầy đủ dịch vụ: khám, spa, boarding.',
   'MX-005', true, 4.9, '07:00', '22:00', 'Mon,Tue,Wed,Thu,Fri,Sat,Sun', 'AUTO');

-- ============================================================
-- SERVICES (6 dịch vụ × 5 shop = 30 dịch vụ)
-- ============================================================
INSERT INTO pet_service (id, shop_id, service_name, category, price, duration_minutes,
                         description, active, camera_enabled, created_at) VALUES
-- ── Shop 1 — Spa & Grooming ───────────────────────────────────
(1,  1, 'Tắm & Sấy cơ bản',       'SPA',       150000,  60,
 'Tắm sạch bằng sữa tắm chuyên dụng, sấy khô hoàn toàn, vệ sinh tai và cắt móng.',
 true, false, NOW()),
(2,  1, 'Cắt tỉa lông toàn thân',  'GROOMING',  250000,  90,
 'Cắt tỉa lông theo yêu cầu hoặc theo giống, tạo kiểu chuyên nghiệp. Bao gồm tắm và sấy.',
 true, false, NOW()),
(3,  1, 'Gói Spa thư giãn',        'SPA',       450000, 120,
 'Tắm thơm, massage toàn thân, dưỡng lông mềm mượt, cắt móng, vệ sinh tai, xịt nước hoa.',
 true, false, NOW()),
(4,  1, 'Vệ sinh răng miệng',      'GROOMING',   80000,  30,
 'Đánh răng bằng kem chuyên dụng, làm sạch cao răng, khử mùi hôi miệng.',
 true, false, NOW()),
(5,  1, 'Tắm trị liệu da',         'SPA',       320000,  90,
 'Tắm bằng dầu gội trị liệu cho thú cưng bị ngứa, viêm da, rụng lông. Ủ lông dưỡng ẩm chuyên sâu.',
 true, false, NOW()),
(6,  1, 'Cắt móng & Vệ sinh tai',  'GROOMING',   60000,  20,
 'Cắt móng an toàn, vệ sinh tai sạch sẽ, ngăn ngừa viêm tai. Nhanh gọn, không cần đặt lịch trước.',
 true, false, NOW()),

-- ── Shop 2 — Phòng khám thú y ────────────────────────────────
(7,  2, 'Khám tổng quát',          'CLINIC',    200000,  45,
 'Kiểm tra sức khỏe toàn diện: tim mạch, hô hấp, tiêu hóa, da lông. Tư vấn dinh dưỡng và lịch tiêm phòng.',
 true, false, NOW()),
(8,  2, 'Tiêm phòng đầy đủ',       'CLINIC',    350000,  30,
 'Tiêm vaccine phòng các bệnh nguy hiểm: Care, Parvo, Dại, Lepto. Cấp sổ tiêm phòng có dấu xác nhận.',
 true, false, NOW()),
(9,  2, 'Phẫu thuật triệt sản',    'CLINIC',   1500000, 180,
 'Phẫu thuật triệt sản an toàn với gây mê chuyên nghiệp. Theo dõi hậu phẫu, tái khám miễn phí sau 7 ngày.',
 true, false, NOW()),
(10, 2, 'Xét nghiệm máu cơ bản',   'CLINIC',    300000,  60,
 'Xét nghiệm công thức máu toàn phần và sinh hóa máu. Phát hiện sớm các bệnh tiềm ẩn, kết quả trong 1 giờ.',
 true, false, NOW()),
(11, 2, 'Siêu âm ổ bụng',          'CLINIC',    400000,  45,
 'Siêu âm kiểm tra các cơ quan nội tạng: gan, thận, lách, bàng quang. Phát hiện u nang, sỏi, dị vật.',
 true, false, NOW()),
(12, 2, 'Điều trị ngoại ký sinh',  'CLINIC',    150000,  30,
 'Điều trị ve, bọ chét, ghẻ, nấm da. Tắm thuốc diệt ký sinh, bôi thuốc chuyên dụng, tư vấn phòng ngừa.',
 true, false, NOW()),

-- ── Shop 3 — Boarding & Hotel ─────────────────────────────────
(13, 3, 'Lưu trú tiêu chuẩn (1 đêm)', 'BOARDING', 200000, 1440,
 'Phòng riêng thoáng mát, ăn uống 3 bữa/ngày theo khẩu phần chuẩn, vui chơi có giám sát, báo cáo ảnh hàng ngày.',
 true, true, NOW()),
(14, 3, 'Lưu trú VIP (1 đêm)',     'BOARDING',  450000, 1440,
 'Phòng VIP điều hòa riêng, giường êm ái, ăn uống theo yêu cầu, camera HD 24/7, video call với chủ, tắm miễn phí.',
 true, true, NOW()),
(15, 3, 'Trông giữ ban ngày (8h)', 'BOARDING',  120000,  480,
 'Trông giữ thú cưng trong ngày làm việc (8 tiếng), vui chơi tự do, ăn nhẹ 2 lần, tắm nếu cần thêm phí.',
 true, false, NOW()),
(16, 3, 'Lưu trú tuần (7 đêm)',    'BOARDING', 1200000, 10080,
 'Gói lưu trú 7 đêm tiết kiệm 15%, phòng tiêu chuẩn, ăn uống đầy đủ, tắm 2 lần/tuần, báo cáo ảnh hàng ngày.',
 true, true, NOW()),
(17, 3, 'Tắm & Grooming tại khách sạn', 'SPA',  200000,   90,
 'Dịch vụ tắm và cắt tỉa ngay tại khách sạn, không cần di chuyển. Tiện lợi cho thú cưng đang lưu trú.',
 true, false, NOW()),
(18, 3, 'Huấn luyện cơ bản (1 buổi)', 'BOARDING', 300000, 60,
 'Huấn luyện các lệnh cơ bản: ngồi, nằm, đứng, đi theo. Phù hợp cho chó từ 3 tháng tuổi trở lên.',
 true, false, NOW()),

-- ── Shop 4 — Beauty Salon ─────────────────────────────────────
(19, 4, 'Tắm thơm & Dưỡng lông',  'SPA',       180000,  75,
 'Tắm bằng sữa tắm cao cấp nhập khẩu, ủ dưỡng lông 15 phút, sấy tạo phồng, lông mềm mượt và thơm lâu.',
 true, false, NOW()),
(20, 4, 'Cắt tỉa theo yêu cầu',   'GROOMING',  300000, 100,
 'Cắt tỉa lông theo mẫu yêu cầu hoặc theo giống chuẩn. Tạo kiểu thời trang, bao gồm tắm và sấy.',
 true, false, NOW()),
(21, 4, 'Nhuộm lông nghệ thuật',  'GROOMING',  500000, 150,
 'Nhuộm lông bằng màu thực phẩm 100% an toàn, không gây hại. Tạo điểm nhấn thời trang, màu bền 4-6 tuần.',
 true, false, NOW()),
(22, 4, 'Gói làm đẹp toàn diện',  'SPA',       650000, 180,
 'Combo đầy đủ: tắm thơm + cắt tỉa + dưỡng lông + cắt móng + vệ sinh tai + nước hoa. Tiết kiệm 20%.',
 true, false, NOW()),
(23, 4, 'Massage thư giãn',        'SPA',       200000,  45,
 'Massage toàn thân giúp thú cưng thư giãn, giảm stress, cải thiện tuần hoàn máu. Kết hợp tinh dầu thiên nhiên.',
 true, false, NOW()),
(24, 4, 'Tắm trị liệu & Dưỡng da','SPA',       380000,  90,
 'Tắm bằng sản phẩm trị liệu chuyên biệt cho da nhạy cảm, dị ứng. Ủ dưỡng ẩm sâu, phục hồi da và lông.',
 true, false, NOW()),

-- ── Shop 5 — Premium Center ───────────────────────────────────
(25, 5, 'Khám & Tư vấn sức khỏe', 'CLINIC',    250000,  45,
 'Khám tổng quát bởi bác sĩ thú y 10 năm kinh nghiệm. Tư vấn dinh dưỡng, lịch tiêm phòng, chăm sóc tại nhà.',
 true, false, NOW()),
(26, 5, 'Gói Spa Premium 5 sao',   'SPA',       600000, 150,
 'Spa cao cấp nhất: tắm thơm hoa hồng, massage đá nóng, ủ dưỡng lông collagen, cắt móng, vệ sinh tai, nước hoa Pháp.',
 true, false, NOW()),
(27, 5, 'Lưu trú Suite (1 đêm)',   'BOARDING',  800000, 1440,
 'Suite riêng với giường đệm cao cấp, TV, điều hòa, camera AI nhận diện cảm xúc, ăn uống 5 sao, video call 24/7.',
 true, true, NOW()),
(28, 5, 'Gói chăm sóc toàn diện', 'MIXED',    1200000,  240,
 'Combo tiết kiệm 20%: khám sức khỏe + gói spa premium + cắt tỉa lông + tư vấn dinh dưỡng cá nhân hóa.',
 true, false, NOW()),
(29, 5, 'Xét nghiệm & Chẩn đoán', 'CLINIC',    800000,  90,
 'Xét nghiệm máu toàn diện + siêu âm + X-quang. Chẩn đoán chính xác với thiết bị hiện đại nhất TP.HCM.',
 true, false, NOW()),
(30, 5, 'Huấn luyện hành vi (1 buổi)', 'MIXED', 400000,  90,
 'Huấn luyện sửa hành vi xấu: cắn, sủa quá mức, đi vệ sinh sai chỗ. Phương pháp tích cực, không dùng hình phạt.',
 true, false, NOW());

-- ============================================================
-- STAFF (2 nhân viên/shop)
-- ============================================================
INSERT INTO staff (id, shop_id, user_id, full_name, role, phone, specialization, is_active) VALUES
(1,  1, 17, 'Staff 1',  'GROOMER',      '0922000001', 'Cắt tỉa lông, spa',          true),
(2,  1, 18, 'Staff 2',  'GROOMER',      '0922000002', 'Tắm và dưỡng lông',          true),
(3,  2, 19, 'Staff 3',  'VETERINARIAN', '0922000003', 'Nội khoa, phẫu thuật',       true),
(4,  2, 20, 'Staff 4',  'VETERINARIAN', '0922000004', 'Nhi khoa, tiêm phòng',       true),
(5,  3, 21, 'Staff 5',  'CARETAKER',    '0922000005', 'Chăm sóc chó lớn',           true),
(6,  3, 22, 'Staff 6',  'CARETAKER',    '0922000006', 'Chăm sóc mèo và chó nhỏ',   true),
(7,  4, 23, 'Staff 7',  'GROOMER',      '0922000007', 'Tạo kiểu lông, nhuộm',       true),
(8,  4, 24, 'Staff 8',  'GROOMER',      '0922000008', 'Spa cao cấp, dưỡng lông',    true),
(9,  5, 25, 'Staff 9',  'VETERINARIAN', '0922000009', 'Chẩn đoán hình ảnh',         true),
(10, 5, 26, 'Staff 10', 'GROOMER',      '0922000010', 'Grooming cao cấp',           true);

-- ============================================================
-- PETS (2 pet/user — thêm sterilized để tránh lỗi NOT NULL)
-- ============================================================
INSERT INTO pet (id, owner_id, name, species, breed, gender, color,
                 sterilized, weight, dob, health_note, is_active) VALUES
(1,  7,  'Pet 1',  'Chó', 'Poodle',           'Đực', 'Trắng',      false, 3.5,  '2021-03-15', 'Khỏe mạnh', true),
(2,  7,  'Pet 2',  'Mèo', 'Anh lông ngắn',    'Cái', 'Xám',        false, 4.2,  '2020-07-20', 'Khỏe mạnh', true),
(3,  8,  'Pet 3',  'Chó', 'Chihuahua',        'Cái', 'Vàng',       false, 2.1,  '2022-01-10', 'Khỏe mạnh', true),
(4,  8,  'Pet 4',  'Mèo', 'Mèo ta',           'Đực', 'Đen trắng',  false, 3.8,  '2019-11-05', 'Khỏe mạnh', true),
(5,  9,  'Pet 5',  'Chó', 'Golden Retriever', 'Đực', 'Vàng',       false, 28.0, '2020-05-22', 'Khỏe mạnh', true),
(6,  9,  'Pet 6',  'Mèo', 'Maine Coon',       'Cái', 'Nâu xám',    false, 6.5,  '2021-09-14', 'Khỏe mạnh', true),
(7,  10, 'Pet 7',  'Chó', 'Shih Tzu',         'Cái', 'Trắng nâu',  false, 5.2,  '2021-06-30', 'Khỏe mạnh', true),
(8,  10, 'Pet 8',  'Mèo', 'Mèo Ba Tư',        'Đực', 'Trắng',      false, 4.8,  '2020-02-18', 'Khỏe mạnh', true),
(9,  11, 'Pet 9',  'Chó', 'Husky',            'Đực', 'Đen trắng',  false, 22.0, '2020-08-12', 'Khỏe mạnh', true),
(10, 11, 'Pet 10', 'Mèo', 'Ragdoll',          'Cái', 'Trắng xanh', false, 5.5,  '2022-04-03', 'Khỏe mạnh', true),
(11, 12, 'Pet 11', 'Chó', 'Maltese',          'Đực', 'Trắng',      false, 3.0,  '2022-07-25', 'Khỏe mạnh', true),
(12, 12, 'Pet 12', 'Mèo', 'Scottish Fold',    'Cái', 'Xám',        false, 3.9,  '2021-12-01', 'Khỏe mạnh', true),
(13, 13, 'Pet 13', 'Chó', 'Corgi',            'Đực', 'Vàng trắng', false, 12.0, '2020-10-17', 'Khỏe mạnh', true),
(14, 13, 'Pet 14', 'Mèo', 'Siamese',          'Cái', 'Kem nâu',    false, 3.5,  '2021-05-08', 'Khỏe mạnh', true),
(15, 14, 'Pet 15', 'Chó', 'Beagle',           'Đực', 'Nâu trắng',  false, 10.5, '2019-12-25', 'Khỏe mạnh', true),
(16, 14, 'Pet 16', 'Mèo', 'Mèo Nhật',         'Đực', 'Cam trắng',  false, 4.1,  '2022-02-14', 'Khỏe mạnh', true),
(17, 15, 'Pet 17', 'Chó', 'Bichon Frise',     'Cái', 'Trắng',      false, 4.5,  '2021-08-20', 'Khỏe mạnh', true),
(18, 15, 'Pet 18', 'Mèo', 'Mèo ta',           'Đực', 'Đen trắng',  false, 3.2,  '2020-06-11', 'Khỏe mạnh', true),
(19, 16, 'Pet 19', 'Chó', 'German Shepherd',  'Đực', 'Đen vàng',   false, 32.0, '2019-09-05', 'Khỏe mạnh', true),
(20, 16, 'Pet 20', 'Mèo', 'Abyssinian',       'Cái', 'Nâu đỏ',     false, 3.7,  '2021-11-30', 'Khỏe mạnh', true);

-- ============================================================
-- BOOKINGS + PAYMENTS
-- ============================================================
INSERT INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id,
                     appointment_datetime, status, note, created_at) VALUES
-- COMPLETED (15)
(1,  7,  1, 1,  1,  1, DATE_SUB(NOW(), INTERVAL 30 DAY), 'COMPLETED', 'Ghi chú booking 1',  DATE_SUB(NOW(), INTERVAL 31 DAY)),
(2,  7,  1, 2,  1,  2, DATE_SUB(NOW(), INTERVAL 25 DAY), 'COMPLETED', 'Ghi chú booking 2',  DATE_SUB(NOW(), INTERVAL 26 DAY)),
(3,  8,  2, 7,  3,  3, DATE_SUB(NOW(), INTERVAL 20 DAY), 'COMPLETED', 'Ghi chú booking 3',  DATE_SUB(NOW(), INTERVAL 21 DAY)),
(4,  8,  2, 8,  4,  4, DATE_SUB(NOW(), INTERVAL 18 DAY), 'COMPLETED', 'Ghi chú booking 4',  DATE_SUB(NOW(), INTERVAL 19 DAY)),
(5,  9,  3, 13, 5,  5, DATE_SUB(NOW(), INTERVAL 15 DAY), 'COMPLETED', 'Ghi chú booking 5',  DATE_SUB(NOW(), INTERVAL 16 DAY)),
(6,  9,  3, 14, 6,  6, DATE_SUB(NOW(), INTERVAL 12 DAY), 'COMPLETED', 'Ghi chú booking 6',  DATE_SUB(NOW(), INTERVAL 13 DAY)),
(7,  10, 4, 19, 7,  7, DATE_SUB(NOW(), INTERVAL 10 DAY), 'COMPLETED', 'Ghi chú booking 7',  DATE_SUB(NOW(), INTERVAL 11 DAY)),
(8,  10, 4, 20, 8,  8, DATE_SUB(NOW(), INTERVAL 8  DAY), 'COMPLETED', 'Ghi chú booking 8',  DATE_SUB(NOW(), INTERVAL 9  DAY)),
(9,  11, 5, 25, 9,  9, DATE_SUB(NOW(), INTERVAL 7  DAY), 'COMPLETED', 'Ghi chú booking 9',  DATE_SUB(NOW(), INTERVAL 8  DAY)),
(10, 11, 5, 26, 10, 10,DATE_SUB(NOW(), INTERVAL 5  DAY), 'COMPLETED', 'Ghi chú booking 10', DATE_SUB(NOW(), INTERVAL 6  DAY)),
(11, 12, 1, 3,  11, 1, DATE_SUB(NOW(), INTERVAL 22 DAY), 'COMPLETED', 'Ghi chú booking 11', DATE_SUB(NOW(), INTERVAL 23 DAY)),
(12, 13, 2, 10, 13, 3, DATE_SUB(NOW(), INTERVAL 14 DAY), 'COMPLETED', 'Ghi chú booking 12', DATE_SUB(NOW(), INTERVAL 15 DAY)),
(13, 14, 3, 15, 15, 5, DATE_SUB(NOW(), INTERVAL 9  DAY), 'COMPLETED', 'Ghi chú booking 13', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(14, 15, 4, 21, 17, 7, DATE_SUB(NOW(), INTERVAL 6  DAY), 'COMPLETED', 'Ghi chú booking 14', DATE_SUB(NOW(), INTERVAL 7  DAY)),
(15, 16, 5, 28, 19, 9, DATE_SUB(NOW(), INTERVAL 3  DAY), 'COMPLETED', 'Ghi chú booking 15', DATE_SUB(NOW(), INTERVAL 4  DAY)),
-- CONFIRMED (5)
(16, 7,  1, 1,  2,  2, DATE_ADD(NOW(), INTERVAL 2  DAY), 'CONFIRMED', 'Ghi chú booking 16', DATE_SUB(NOW(), INTERVAL 1  DAY)),
(17, 8,  2, 7,  3,  3, DATE_ADD(NOW(), INTERVAL 3  DAY), 'CONFIRMED', 'Ghi chú booking 17', DATE_SUB(NOW(), INTERVAL 1  DAY)),
(18, 9,  3, 14, 5,  5, DATE_ADD(NOW(), INTERVAL 5  DAY), 'CONFIRMED', 'Ghi chú booking 18', DATE_SUB(NOW(), INTERVAL 1  DAY)),
(19, 10, 4, 20, 7,  7, DATE_ADD(NOW(), INTERVAL 1  DAY), 'CONFIRMED', 'Ghi chú booking 19', NOW()),
(20, 11, 5, 26, 9,  10,DATE_ADD(NOW(), INTERVAL 4  DAY), 'CONFIRMED', 'Ghi chú booking 20', NOW()),
-- IN_PROGRESS (2)
(21, 12, 1, 2,  11, 1, NOW(), 'IN_PROGRESS', 'Ghi chú booking 21', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(22, 13, 2, 7,  13, 3, NOW(), 'IN_PROGRESS', 'Ghi chú booking 22', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
-- CANCELLED (2)
(23, 14, 3, 13, 15, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), 'CANCELLED', 'Ghi chú booking 23', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(24, 15, 4, 19, 17, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), 'CANCELLED', 'Ghi chú booking 24', DATE_SUB(NOW(), INTERVAL 4 DAY));

INSERT INTO payment (id, booking_id, amount, method, status, description, payment_time) VALUES
(1,  1,  150000,  'CASH',  'SUCCESS',   'Payment booking 1',  DATE_SUB(NOW(), INTERVAL 30 DAY)),
(2,  2,  250000,  'PAYOS', 'SUCCESS',   'Payment booking 2',  DATE_SUB(NOW(), INTERVAL 25 DAY)),
(3,  3,  200000,  'CASH',  'SUCCESS',   'Payment booking 3',  DATE_SUB(NOW(), INTERVAL 20 DAY)),
(4,  4,  350000,  'PAYOS', 'SUCCESS',   'Payment booking 4',  DATE_SUB(NOW(), INTERVAL 18 DAY)),
(5,  5,  200000,  'CASH',  'SUCCESS',   'Payment booking 5',  DATE_SUB(NOW(), INTERVAL 15 DAY)),
(6,  6,  450000,  'PAYOS', 'SUCCESS',   'Payment booking 6',  DATE_SUB(NOW(), INTERVAL 12 DAY)),
(7,  7,  180000,  'CASH',  'SUCCESS',   'Payment booking 7',  DATE_SUB(NOW(), INTERVAL 10 DAY)),
(8,  8,  300000,  'PAYOS', 'SUCCESS',   'Payment booking 8',  DATE_SUB(NOW(), INTERVAL 8  DAY)),
(9,  9,  250000,  'CASH',  'SUCCESS',   'Payment booking 9',  DATE_SUB(NOW(), INTERVAL 7  DAY)),
(10, 10, 600000,  'PAYOS', 'SUCCESS',   'Payment booking 10', DATE_SUB(NOW(), INTERVAL 5  DAY)),
(11, 11, 450000,  'CASH',  'SUCCESS',   'Payment booking 11', DATE_SUB(NOW(), INTERVAL 22 DAY)),
(12, 12, 300000,  'PAYOS', 'SUCCESS',   'Payment booking 12', DATE_SUB(NOW(), INTERVAL 14 DAY)),
(13, 13, 120000,  'CASH',  'SUCCESS',   'Payment booking 13', DATE_SUB(NOW(), INTERVAL 9  DAY)),
(14, 14, 500000,  'PAYOS', 'SUCCESS',   'Payment booking 14', DATE_SUB(NOW(), INTERVAL 6  DAY)),
(15, 15, 1200000, 'PAYOS', 'SUCCESS',   'Payment booking 15', DATE_SUB(NOW(), INTERVAL 3  DAY)),
(16, 16, 150000,  'CASH',  'PENDING',   'Payment booking 16', NOW()),
(17, 17, 200000,  'CASH',  'PENDING',   'Payment booking 17', NOW()),
(18, 18, 450000,  'PAYOS', 'PENDING',   'Payment booking 18', NOW()),
(19, 19, 300000,  'CASH',  'PENDING',   'Payment booking 19', NOW()),
(20, 20, 600000,  'PAYOS', 'PENDING',   'Payment booking 20', NOW()),
(21, 21, 250000,  'CASH',  'PENDING',   'Payment booking 21', NOW()),
(22, 22, 200000,  'CASH',  'PENDING',   'Payment booking 22', NOW()),
(23, 23, 200000,  'CASH',  'CANCELLED', 'Payment booking 23', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(24, 24, 180000,  'CASH',  'CANCELLED', 'Payment booking 24', DATE_SUB(NOW(), INTERVAL 3 DAY));

-- ============================================================
-- REVIEWS
-- ============================================================
INSERT INTO review (id, shop_id, user_id, service_id, rating, comment, created_at, reply, replied_at) VALUES
(1,  1, 7,  1, 5, 'Dịch vụ tốt, nhân viên tận tình. Sẽ quay lại!',
    DATE_SUB(NOW(), INTERVAL 29 DAY), 'Cảm ơn bạn đã tin tưởng Shop 1!', DATE_SUB(NOW(), INTERVAL 28 DAY)),
(2,  1, 7,  2, 5, 'Cắt tỉa đẹp, đúng yêu cầu. Giá hợp lý.',
    DATE_SUB(NOW(), INTERVAL 24 DAY), 'Cảm ơn bạn! Hẹn gặp lại.', DATE_SUB(NOW(), INTERVAL 23 DAY)),
(3,  2, 8,  7, 5, 'Bác sĩ khám kỹ, giải thích rõ ràng. Phòng khám sạch sẽ.',
    DATE_SUB(NOW(), INTERVAL 19 DAY), 'Cảm ơn bạn đã đánh giá!', DATE_SUB(NOW(), INTERVAL 18 DAY)),
(4,  2, 8,  8, 4, 'Tiêm phòng nhanh, bé không quấy. Hơi đợi lâu.',
    DATE_SUB(NOW(), INTERVAL 17 DAY), 'Cảm ơn góp ý, chúng tôi sẽ cải thiện!', DATE_SUB(NOW(), INTERVAL 16 DAY)),
(5,  3, 9,  13, 4, 'Phòng sạch, nhân viên chăm sóc tốt.',
    DATE_SUB(NOW(), INTERVAL 14 DAY), 'Cảm ơn bạn!', DATE_SUB(NOW(), INTERVAL 13 DAY)),
(6,  3, 9,  14, 5, 'Phòng VIP xứng đáng. Camera rõ nét, xem được bé mọi lúc.',
    DATE_SUB(NOW(), INTERVAL 11 DAY), 'Cảm ơn bạn đã tin tưởng Shop 3!', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(7,  4, 10, 19, 5, 'Tắm thơm, lông mềm mượt cả tuần!',
    DATE_SUB(NOW(), INTERVAL 9  DAY), 'Cảm ơn bạn!', DATE_SUB(NOW(), INTERVAL 8  DAY)),
(8,  4, 10, 20, 4, 'Cắt đẹp nhưng mất nhiều thời gian. Kết quả xứng đáng.',
    DATE_SUB(NOW(), INTERVAL 7  DAY), 'Cảm ơn góp ý!', DATE_SUB(NOW(), INTERVAL 6  DAY)),
(9,  5, 11, 25, 5, 'Bác sĩ chuyên nghiệp, trang thiết bị hiện đại.',
    DATE_SUB(NOW(), INTERVAL 6  DAY), 'Cảm ơn bạn!', DATE_SUB(NOW(), INTERVAL 5  DAY)),
(10, 5, 11, 26, 5, 'Gói spa tuyệt vời! Thú cưng thư giãn hoàn toàn.',
    DATE_SUB(NOW(), INTERVAL 4  DAY), 'Cảm ơn bạn đã chọn Shop 5!', DATE_SUB(NOW(), INTERVAL 3  DAY)),
(11, 1, 12, 3,  5, 'Spa cao cấp rất đáng tiền.',
    DATE_SUB(NOW(), INTERVAL 21 DAY), NULL, NULL),
(12, 2, 13, 10, 5, 'Xét nghiệm nhanh, kết quả rõ ràng.',
    DATE_SUB(NOW(), INTERVAL 13 DAY), NULL, NULL),
(13, 3, 14, 15, 4, 'Trông giữ tốt, giá hợp lý.',
    DATE_SUB(NOW(), INTERVAL 8  DAY), NULL, NULL),
(14, 4, 15, 21, 5, 'Nhuộm lông rất đẹp!',
    DATE_SUB(NOW(), INTERVAL 5  DAY), NULL, NULL),
(15, 5, 16, 28, 5, 'Gói toàn diện tiết kiệm và chất lượng.',
    DATE_SUB(NOW(), INTERVAL 2  DAY), NULL, NULL);

-- Cập nhật rating_avg
UPDATE shop SET rating_avg = (
    SELECT ROUND(AVG(r.rating), 1) FROM review r WHERE r.shop_id = shop.id
) WHERE id IN (1,2,3,4,5);

-- ============================================================
-- SHOP WALLETS (90% từ booking COMPLETED)
-- ============================================================
INSERT INTO shop_wallet (id, shop_id, frozen_balance, available_balance, total_earned, total_withdrawn, updated_at) VALUES
(1, 1, 0.00,  765000.00,  765000.00,  0.00, NOW()),  -- (150k+250k+450k)×90%
(2, 2, 0.00,  765000.00,  765000.00,  0.00, NOW()),  -- (200k+350k+300k)×90%
(3, 3, 0.00,  693000.00,  693000.00,  0.00, NOW()),  -- (200k+450k+120k)×90%
(4, 4, 0.00,  882000.00,  882000.00,  0.00, NOW()),  -- (180k+300k+500k)×90%
(5, 5, 0.00, 1845000.00, 1845000.00,  0.00, NOW());  -- (250k+600k+1200k)×90%

-- ============================================================
-- NOTIFICATIONS
-- ============================================================
INSERT INTO notification (user_id, title, content, broadcast_id, is_read, notification_type, created_at) VALUES
(7,  'Chào mừng đến Peteye! 🐾', 'Cảm ơn bạn đã đăng ký. Khám phá dịch vụ ngay hôm nay!', 'welcome-001', false, 'GENERAL',   DATE_SUB(NOW(), INTERVAL 30 DAY)),
(8,  'Chào mừng đến Peteye! 🐾', 'Cảm ơn bạn đã đăng ký. Khám phá dịch vụ ngay hôm nay!', 'welcome-001', true,  'GENERAL',   DATE_SUB(NOW(), INTERVAL 30 DAY)),
(9,  'Chào mừng đến Peteye! 🐾', 'Cảm ơn bạn đã đăng ký. Khám phá dịch vụ ngay hôm nay!', 'welcome-001', true,  'GENERAL',   DATE_SUB(NOW(), INTERVAL 30 DAY)),
(7,  'Dịch vụ hoàn thành ✅', 'Dịch vụ Tắm & Sấy cho Pet 1 tại Shop 1 đã hoàn thành!', NULL, true,  'BOOKING',   DATE_SUB(NOW(), INTERVAL 30 DAY)),
(8,  'Dịch vụ hoàn thành ✅', 'Dịch vụ Khám tổng quát cho Pet 3 tại Shop 2 đã hoàn thành!', NULL, true,  'BOOKING',   DATE_SUB(NOW(), INTERVAL 20 DAY)),
(9,  'Dịch vụ hoàn thành ✅', 'Pet 5 đã hoàn thành kỳ lưu trú tại Shop 3!', NULL, true,  'BOOKING',   DATE_SUB(NOW(), INTERVAL 15 DAY)),
(7,  'Nhắc lịch hẹn 📅', 'Bạn có lịch hẹn tại Shop 1 vào ngày mai. Đừng quên nhé!', NULL, false, 'REMINDER',  DATE_SUB(NOW(), INTERVAL 1  DAY)),
(8,  'Nhắc lịch hẹn 📅', 'Bạn có lịch khám tại Shop 2 sau 3 ngày nữa.', NULL, false, 'REMINDER',  DATE_SUB(NOW(), INTERVAL 1  DAY)),
(7,  'Ưu đãi đặc biệt 🎉', 'Giảm 20% dịch vụ Spa tháng này tại Shop 1!', 'promo-001', false, 'PROMOTION', DATE_SUB(NOW(), INTERVAL 5  DAY)),
(8,  'Ưu đãi đặc biệt 🎉', 'Giảm 20% dịch vụ Spa tháng này tại Shop 1!', 'promo-001', false, 'PROMOTION', DATE_SUB(NOW(), INTERVAL 5  DAY)),
(2,  'Đơn hàng mới 🛎️', 'Có lịch đặt mới từ User 1 cho dịch vụ Tắm & Sấy.', NULL, true,  'BOOKING',   DATE_SUB(NOW(), INTERVAL 1  DAY)),
(3,  'Đơn hàng mới 🛎️', 'Có lịch đặt mới từ User 2 cho dịch vụ Khám tổng quát.', NULL, false, 'BOOKING',   DATE_SUB(NOW(), INTERVAL 1  DAY));

-- ============================================================
-- VERIFY
-- ============================================================
SELECT 'USERS'    AS entity, COUNT(*) AS total FROM user;
SELECT 'SHOPS'    AS entity, COUNT(*) AS total FROM shop;
SELECT 'SERVICES' AS entity, COUNT(*) AS total FROM pet_service;
SELECT 'STAFF'    AS entity, COUNT(*) AS total FROM staff;
SELECT 'PETS'     AS entity, COUNT(*) AS total FROM pet;
SELECT 'BOOKINGS' AS entity, COUNT(*) AS total FROM booking;
SELECT 'PAYMENTS' AS entity, COUNT(*) AS total FROM payment;
SELECT 'REVIEWS'  AS entity, COUNT(*) AS total FROM review;
SELECT 'WALLETS'  AS entity, COUNT(*) AS total FROM shop_wallet;

-- ============================================================
-- ACCOUNT SUMMARY
-- ============================================================
SELECT '=== TÀI KHOẢN MẪU (mật khẩu: 12345678) ===' AS info;
SELECT 'admin@peteye.com'  AS email, 'ADMIN'      AS role, 'Đăng nhập trang Admin'       AS note UNION ALL
SELECT 'shop1@peteye.com', 'SHOP_OWNER', 'Đăng nhập trang Shop (Shop 1 — Spa Q.5)'       UNION ALL
SELECT 'shop2@peteye.com', 'SHOP_OWNER', 'Đăng nhập trang Shop (Shop 2 — Clinic Q.3)'    UNION ALL
SELECT 'shop3@peteye.com', 'SHOP_OWNER', 'Đăng nhập trang Shop (Shop 3 — Boarding BT)'   UNION ALL
SELECT 'shop4@peteye.com', 'SHOP_OWNER', 'Đăng nhập trang Shop (Shop 4 — Spa PN)'        UNION ALL
SELECT 'shop5@peteye.com', 'SHOP_OWNER', 'Đăng nhập trang Shop (Shop 5 — Mixed Q.1)'     UNION ALL
SELECT 'user1@gmail.com',  'USER',       'Đăng nhập trang User'                           UNION ALL
SELECT 'staff1@peteye.com','STAFF',      'Đăng nhập trang Staff (Shop 1)';
