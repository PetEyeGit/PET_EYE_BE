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


-- ==========================================
-- UPDATE SHOP WALLET BALANCES FOR TESTING REFUND
-- ==========================================
UPDATE shop_wallet SET available_balance = 5000000, total_earned = 5000000 WHERE id IN (1, 2);

-- ==========================================
-- PETS
-- ==========================================
INSERT IGNORE INTO pet (id, owner_id, name, species, breed, gender, color, avatar, sterilized, weight, dob, health_note, favorite_food, allergies, hobbies, walk_time, is_active)
VALUES (991, 4, 'Bé Lu', 'DOG', 'Poodle', 'MALE', 'Nâu', 'https://images.unsplash.com/photo-1543466835-00a7907e9de1', 1, 4.5, '2020-01-01', 'Khỏe mạnh', 'Hạt Royal Canin', 'Không', 'Chạy nhảy', '17:00', 1);

INSERT IGNORE INTO pet (id, owner_id, name, species, breed, gender, color, avatar, sterilized, weight, dob, health_note, favorite_food, allergies, hobbies, walk_time, is_active)
VALUES (992, 5, 'Mimi', 'CAT', 'Anh lông ngắn', 'FEMALE', 'Xám', 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba', 1, 3.2, '2021-05-10', 'Dễ rụng lông', 'Pate', 'Hải sản', 'Ngủ', 'Không', 1);

-- ==========================================
-- BOOKINGS (for refund testing)
-- ==========================================
-- Booking 1: WAITING_REFUND (Shop approved cancel, waiting admin to refund) for Customer 1, Shop 1, Service 1 (Price: 250000)
INSERT IGNORE INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, cancellation_reason, bank_name, bank_account, account_holder, payos_order_code, created_at)
VALUES (991, 4, 1, 1, 991, 1, DATE_ADD(NOW(), INTERVAL 2 DAY), 'WAITING_REFUND', 'Test Refund 1', 'Đổi ý phút chót', 'Vietcombank', '0123456789', 'NGUYEN VAN KHACH 1', 100001, DATE_ADD(NOW(), INTERVAL -1 DAY));

-- Booking 2: CANCEL_REQUESTED (Customer requested cancel, waiting shop to approve) for Customer 2, Shop 2, Service 5 (Price: 120000)
INSERT IGNORE INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, cancellation_reason, bank_name, bank_account, account_holder, payos_order_code, created_at)
VALUES (992, 5, 2, 5, 992, 3, DATE_ADD(NOW(), INTERVAL 3 DAY), 'CANCEL_REQUESTED', 'Test Cancel Request', 'Kẹt lịch trình', 'MBBank', '9876543210', 'TRAN THI KHACH 2', 100002, NOW());

-- Booking 3: CONFIRMED (Upcoming, can be cancelled) for Customer 1, Shop 1, Service 2 (Price: 450000)
INSERT IGNORE INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, cancellation_reason, bank_name, bank_account, account_holder, payos_order_code, created_at)
VALUES (993, 4, 1, 2, 991, 2, DATE_ADD(NOW(), INTERVAL 5 DAY), 'CONFIRMED', 'Test hủy lịch mới', NULL, NULL, NULL, NULL, 100003, NOW());

-- Booking 4: COMPLETED (Done) for Customer 1, Shop 1, Service 3 (Price: 600000)
INSERT IGNORE INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, cancellation_reason, bank_name, bank_account, account_holder, payos_order_code, created_at)
VALUES (994, 4, 1, 3, 991, 1, DATE_ADD(NOW(), INTERVAL -2 DAY), 'COMPLETED', 'Đã khám', NULL, NULL, NULL, NULL, 100004, DATE_ADD(NOW(), INTERVAL -5 DAY));


-- Booking 5: CONFIRMED (CASH_DEPOSIT 10%) for Customer 1, Shop 1, Service 2 (Price: 450000)
INSERT IGNORE INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, cancellation_reason, bank_name, bank_account, account_holder, payos_order_code, created_at)
VALUES (995, 4, 1, 2, 991, 2, DATE_ADD(NOW(), INTERVAL 4 DAY), 'CONFIRMED', 'Test CASH DEPOSIT', NULL, NULL, NULL, NULL, 100005, NOW());
INSERT IGNORE INTO payment (id, booking_id, method, amount, status, checkout_url, payment_time)
VALUES (995, 995, 'CASH_DEPOSIT', 45000, 'PAID', 'http://payos.vn/dummy', NOW());

-- Booking 6: CONFIRMED (PAYOS 100%) for Customer 1, Shop 1, Service 2 (Price: 450000)
INSERT IGNORE INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, payos_order_code, created_at)
VALUES (996, 4, 1, 2, 991, 2, DATE_ADD(NOW(), INTERVAL 4 DAY), 'CONFIRMED', 'Test PAYOS 100%', 100006, NOW());
INSERT IGNORE INTO payment (id, booking_id, method, amount, status, checkout_url, payment_time)
VALUES (996, 996, 'PAYOS', 450000, 'PAID', 'http://payos.vn/dummy', NOW());

-- Booking 7: IN_PROGRESS for Customer 1, Shop 1, Service 2 (Price: 450000)
INSERT IGNORE INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, payos_order_code, created_at)
VALUES (997, 4, 1, 2, 991, 2, DATE_ADD(NOW(), INTERVAL -1 DAY), 'IN_PROGRESS', 'Đang thực hiện', 100007, NOW());
INSERT IGNORE INTO payment (id, booking_id, method, amount, status, checkout_url, payment_time)
VALUES (997, 997, 'PAYOS', 450000, 'PAID', 'http://payos.vn/dummy', NOW());

-- Booking 8: CANCELLED (CASH_DEPOSIT) for Customer 1, Shop 1, Service 2 (Price: 450000)
INSERT IGNORE INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, cancellation_reason, payos_order_code, created_at)
VALUES (998, 4, 1, 2, 991, 2, DATE_ADD(NOW(), INTERVAL 4 DAY), 'CANCELLED', 'Đã hủy', 'Bận đột xuất', 100008, NOW());
INSERT IGNORE INTO payment (id, booking_id, method, amount, status, checkout_url, payment_time)
VALUES (998, 998, 'CASH_DEPOSIT', 45000, 'PAID', 'http://payos.vn/dummy', NOW());




-- Booking 9: CONFIRMED (Test No-Show: Late 10 mins -> Error TOO EARLY) for Customer 1, Shop 1, Service 2
INSERT IGNORE INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, cancellation_reason, payos_order_code, created_at)
VALUES (999, 4, 1, 2, 991, 2, DATE_ADD(NOW(), INTERVAL -10 MINUTE), 'CONFIRMED', 'Test No-Show Chưa tới hạn', NULL, 100009, NOW());
INSERT IGNORE INTO payment (id, booking_id, method, amount, status, checkout_url, payment_time)
VALUES (999, 999, 'PAYOS', 450000, 'PAID', 'http://payos.vn/dummy', NOW());

-- Booking 10: CONFIRMED (Test No-Show: Late 20 mins -> Success) for Customer 1, Shop 1, Service 2
INSERT IGNORE INTO booking (id, user_id, shop_id, service_id, pet_id, staff_id, appointment_datetime, status, note, cancellation_reason, payos_order_code, created_at)
VALUES (1000, 4, 1, 2, 991, 2, DATE_ADD(NOW(), INTERVAL -20 MINUTE), 'CONFIRMED', 'Test No-Show Đã quá hạn', NULL, 100010, NOW());
INSERT IGNORE INTO payment (id, booking_id, method, amount, status, checkout_url, payment_time)
VALUES (1000, 1000, 'PAYOS', 450000, 'PAID', 'http://payos.vn/dummy', NOW());

-- Mock Transactions for existing Bookings
INSERT IGNORE INTO transaction (id, booking_id, shop_id, type, amount, payment_method, status, description, created_at)
VALUES (995, 995, 1, 'BOOKING_PAYMENT', 45000, 'CASH_DEPOSIT', 'SUCCESS', 'Thanh toán cọc 10%', NOW());
INSERT IGNORE INTO transaction (id, booking_id, shop_id, type, amount, payment_method, status, description, created_at)
VALUES (996, 996, 1, 'BOOKING_PAYMENT', 450000, 'PAYOS', 'SUCCESS', 'Thanh toán 100% qua PayOS', NOW());
INSERT IGNORE INTO transaction (id, booking_id, shop_id, type, amount, payment_method, status, description, created_at)
VALUES (998, 998, 1, 'REFUND', 45000, 'CASH_DEPOSIT', 'FAILED', 'Hoàn tiền cọc (thất bại do quy định)', NOW());
INSERT IGNORE INTO transaction (id, booking_id, shop_id, type, amount, payment_method, status, description, created_at)
VALUES (999, 999, 1, 'BOOKING_PAYMENT', 450000, 'PAYOS', 'SUCCESS', 'Thanh toán 100% qua PayOS (Booking 999)', NOW());
INSERT IGNORE INTO transaction (id, booking_id, shop_id, type, amount, payment_method, status, description, created_at)
VALUES (1000, 1000, 1, 'BOOKING_PAYMENT', 450000, 'PAYOS', 'SUCCESS', 'Thanh toán 100% qua PayOS (Booking 1000)', NOW());

-- ==========================================
-- PET IMAGES (ALBUM)
-- ==========================================
INSERT IGNORE INTO pet_image (id, pet_id, image_url, description, upload_date)
VALUES (1, 991, 'https://images.unsplash.com/photo-1543466835-00a7907e9de1', 'Ảnh chân dung Bé Lu tại nhà', DATE_ADD(NOW(), INTERVAL -10 DAY));

INSERT IGNORE INTO pet_image (id, pet_id, image_url, description, upload_date)
VALUES (2, 991, 'https://images.unsplash.com/photo-1583337130417-3346a1be7dee', 'Bé Lu đi dạo công viên', DATE_ADD(NOW(), INTERVAL -5 DAY));

INSERT IGNORE INTO pet_image (id, pet_id, image_url, description, upload_date)
VALUES (3, 992, 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba', 'Mimi ngủ nướng', DATE_ADD(NOW(), INTERVAL -2 DAY));

-- ==========================================
-- TEST NOTIFICATIONS
-- ==========================================
INSERT IGNORE INTO notification (id, user_id, title, content, broadcast_id, notification_type, is_read, created_at)
VALUES 
(1, 6, 'Có đơn hàng mới cần xử lý', 'Một khách hàng vừa đặt lịch Tắm & Spa Toàn Diện. Hãy vào kiểm tra và nhận ca ngay nhé!', 'bcast-001', 'BOOKING', 0, NOW()),
(2, 6, 'Khách hàng thay đổi lịch hẹn', 'Đơn hàng #1002 đã được dời sang 15:00 ngày mai. Vui lòng sắp xếp thời gian.', 'bcast-002', 'BOOKING', 0, NOW()),
(3, 7, 'Khách hàng thay đổi lịch hẹn', 'Đơn hàng #1002 đã được dời sang 15:00 ngày mai. Vui lòng sắp xếp thời gian.', 'bcast-002', 'BOOKING', 0, NOW()),
(4, 7, 'Có đơn hàng mới cần xử lý', 'Một khách hàng vừa đặt lịch Tắm & Spa Toàn Diện. Hãy vào kiểm tra và nhận ca ngay nhé!', 'bcast-001', 'BOOKING', 0, NOW()),
(5, 7, 'Nhắc nhở công việc', 'Sắp tới giờ thực hiện dịch vụ cho bé Lu. Vui lòng chuẩn bị dụng cụ đầy đủ.', 'bcast-003', 'REMINDER', 0, NOW()),
(6, 7, 'Thưởng tháng 5', 'Chúc mừng bạn đã hoàn thành xuất sắc chỉ tiêu tháng. Bạn được thưởng nóng 500k!', 'bcast-004', 'SYSTEM', 0, NOW()),
(7, 7, 'Cập nhật hệ thống', 'Hệ thống Workspace v2.0 đã cập nhật thêm tính năng lọc trạng thái đơn. Trải nghiệm ngay!', 'bcast-005', 'SYSTEM', 0, NOW()),
(8, 4, 'Đơn hàng hoàn tất', 'Dịch vụ của bé đã hoàn tất. Vui lòng đánh giá dịch vụ.', 'bcast-006', 'BOOKING', 0, NOW()),
(9, 2, 'Có đơn hàng mới', 'Cửa hàng của bạn vừa có 1 đơn hàng mới.', 'bcast-007', 'BOOKING', 0, NOW());

-- ==========================================
-- TEST CARE LOGS
-- ==========================================
INSERT IGNORE INTO care_log (id, booking_id, staff_id, type, note, timestamp)
VALUES 
(1, 997, 1, 'CLEANING', 'Đã tắm rửa vệ sinh sạch sẽ cho bé. Bé rất ngoan và hợp tác.', DATE_ADD(NOW(), INTERVAL -1 HOUR)),
(2, 997, 1, 'FEEDING', 'Đã cho bé ăn hạt cao cấp theo đúng khẩu phần khách dặn.', DATE_ADD(NOW(), INTERVAL -30 MINUTE)),
(3, 997, 1, 'MEDICAL', 'Đã kiểm tra tổng quát, da và lông bé đều khỏe mạnh không có ve rận.', DATE_ADD(NOW(), INTERVAL -10 MINUTE)),
(4, 996, 2, 'EXERCISE', 'Bé đã được chạy bộ 15 phút tại sân chơi của shop.', DATE_ADD(NOW(), INTERVAL -2 HOUR)),
(5, 996, 2, 'CLEANING', 'Đã chải chuốt lông rụng và cắt móng gọn gàng.', DATE_ADD(NOW(), INTERVAL -1 HOUR));
