-- ============================================================================
-- COMPLETE DATABASE RESET AND SEED DATA (1-CLICK RUN FOR DBEAVER)
-- ============================================================================

-- ============================================================================
-- PHẦN 1: DROP VÀ TẠO LẠI DATABASE
-- ============================================================================
DROP DATABASE IF EXISTS PET_EYE;
CREATE DATABASE PET_EYE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE PET_EYE;

-- Tắt kiểm tra khóa ngoại để tạo bảng và chèn data không bị xung đột
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- PHẦN 2: ĐỊNH NGHĨA KHUNG CẤU TRÚC BẢNG (DDL)
-- Nhờ phần này, bạn KHÔNG cần phải chạy Spring Boot trước nữa.
-- ============================================================================

CREATE TABLE IF NOT EXISTS `role` (
                                      `id` INT NOT NULL AUTO_INCREMENT,
                                      `name` VARCHAR(50) NOT NULL,
    `description` VARCHAR(255),
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `user` (
                                      `id` INT NOT NULL AUTO_INCREMENT,
                                      `email` VARCHAR(100) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(100),
    `phone` VARCHAR(20),
    `address` VARCHAR(255),
    `avatar` VARCHAR(255),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `active` TINYINT(1) DEFAULT 1,
    `email_verified` TINYINT(1) DEFAULT 0,
    `just_upgraded` TINYINT(1) DEFAULT 0,
    `tier_id` INT,
    `total_spending` DOUBLE DEFAULT 0,
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `user_roles` (
                                            `user_id` INT NOT NULL,
                                            `roles_id` INT NOT NULL,
                                            PRIMARY KEY (`user_id`, `roles_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`roles_id`) REFERENCES `role`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `shop` (
                                      `id` INT NOT NULL AUTO_INCREMENT,
                                      `owner_id` INT,
                                      `shop_name` VARCHAR(150),
    `shop_type` VARCHAR(50),
    `email` VARCHAR(100),
    `phone` VARCHAR(20),
    `address` VARCHAR(255),
    `city` VARCHAR(100),
    `latitude` DOUBLE,
    `longitude` DOUBLE,
    `description` TEXT,
    `license_number` VARCHAR(100),
    `license_image_url` VARCHAR(255),
    `logo_url` VARCHAR(255),
    `banner_url` VARCHAR(255),
    `gallery_urls` TEXT,
    `open_time` VARCHAR(10),
    `close_time` VARCHAR(10),
    `working_days` VARCHAR(255),
    `rating_avg` DOUBLE DEFAULT 0,
    `is_verified` TINYINT(1) DEFAULT 0,
    `status` VARCHAR(50),
    `assignment_mode` VARCHAR(50),
    `late_grace_period` INT,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`owner_id`) REFERENCES `user`(`id`) ON DELETE SET NULL
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `staff` (
                                       `id` INT NOT NULL AUTO_INCREMENT,
                                       `shop_id` INT,
                                       `user_id` INT,
                                       `full_name` VARCHAR(100),
    `role` VARCHAR(100),
    `phone` VARCHAR(20),
    `specialization` VARCHAR(255),
    `is_active` TINYINT(1) DEFAULT 1,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`shop_id`) REFERENCES `shop`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `pet` (
                                     `id` INT NOT NULL AUTO_INCREMENT,
                                     `owner_id` INT,
                                     `name` VARCHAR(100),
    `species` VARCHAR(50),
    `breed` VARCHAR(100),
    `gender` VARCHAR(20),
    `color` VARCHAR(50),
    `avatar` VARCHAR(255),
    `sterilized` TINYINT(1) DEFAULT 0,
    `weight` DOUBLE,
    `dob` DATE,
    `health_note` TEXT,
    `favorite_food` VARCHAR(255),
    `allergies` VARCHAR(255),
    `hobbies` VARCHAR(255),
    `walk_time` VARCHAR(100),
    `is_active` TINYINT(1) DEFAULT 1,
    `unactive_reason` VARCHAR(255),
    PRIMARY KEY (`id`),
    FOREIGN KEY (`owner_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `pet_service` (
                                             `id` INT NOT NULL AUTO_INCREMENT,
                                             `shop_id` INT,
                                             `service_name` VARCHAR(150),
    `category` VARCHAR(50),
    `price` DOUBLE,
    `duration_minutes` INT,
    `description` TEXT,
    `image_url` VARCHAR(255),
    `active` TINYINT(1) DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `camera_enabled` TINYINT(1) DEFAULT 0,
    `cage_size` VARCHAR(50),
    `room_type` VARCHAR(50),
    `camera_tiers` TEXT,
    `camera_tier_prices` TEXT,
    `camera_tier_labels` TEXT,
    `camera_description` TEXT,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`shop_id`) REFERENCES `shop`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `voucher` (
                                         `id` INT NOT NULL AUTO_INCREMENT,
                                         `code` VARCHAR(50) NOT NULL UNIQUE,
    `discount_type` VARCHAR(50),
    `discount_value` DOUBLE,
    `min_order_value` DOUBLE,
    `max_discount_amount` DOUBLE,
    `valid_days` INT,
    `issue_quantity` INT,
    `target_tier_id` INT,
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `booking` (
                                         `id` INT NOT NULL AUTO_INCREMENT,
                                         `user_id` INT,
                                         `shop_id` INT,
                                         `pet_id` INT,
                                         `staff_id` INT,
                                         `appointment_datetime` DATETIME,
                                         `check_out_datetime` DATETIME,
                                         `service_start_datetime` DATETIME,
                                         `service_end_datetime` DATETIME,
                                         `cage_size` VARCHAR(50),
    `room_type` VARCHAR(50),
    `status` VARCHAR(50),
    `note` TEXT,
    `voucher_id` INT,
    `discount_amount` DOUBLE DEFAULT 0,
    `cancellation_reason` VARCHAR(255),
    `bank_name` VARCHAR(100),
    `bank_account` VARCHAR(50),
    `account_holder` VARCHAR(100),
    `rtsp_link` VARCHAR(255),
    `completed_service_ids` VARCHAR(255),
    `completed_service_times` TEXT,
    `payos_order_code` BIGINT,
    `camera_rtsp_url` VARCHAR(255),
    `camera_stream_url` VARCHAR(255),
    `camera_configured_at` DATETIME,
    `check_in` DATETIME,
    `check_out` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `booking_services` (
                                                  `booking_id` INT NOT NULL,
                                                  `service_id` INT NOT NULL,
                                                  PRIMARY KEY (`booking_id`, `service_id`)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `payment` (
                                         `id` INT NOT NULL AUTO_INCREMENT,
                                         `booking_id` INT,
                                         `amount` DOUBLE,
                                         `method` VARCHAR(50),
    `status` VARCHAR(50),
    `payos_order_code` BIGINT,
    `checkout_url` VARCHAR(255),
    `gateway_transaction_id` VARCHAR(150),
    `payment_time` DATETIME,
    `description` TEXT,
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `pet_medical_record` (
                                                    `id` INT NOT NULL AUTO_INCREMENT,
                                                    `pet_id` INT,
                                                    `booking_id` INT,
                                                    `staff_id` INT,
                                                    `diagnosis` TEXT,
                                                    `treatment` TEXT,
                                                    `prescription` TEXT,
                                                    `visit_date` DATETIME,
                                                    `veterinarian_note` TEXT,
                                                    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `care_log` (
                                          `id` INT NOT NULL AUTO_INCREMENT,
                                          `booking_id` INT,
                                          `staff_id` INT,
                                          `type` VARCHAR(50),
    `note` TEXT,
    `timestamp` DATETIME,
    `image_url` VARCHAR(255),
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `chat_history` (
                                              `id` INT NOT NULL AUTO_INCREMENT,
                                              `user_id` INT,
                                              `role` VARCHAR(50),
    `content` TEXT,
    `tool_result_json` TEXT,
    `created_at` DATETIME,
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `notification` (
                                              `id` INT NOT NULL AUTO_INCREMENT,
                                              `user_id` INT,
                                              `title` VARCHAR(150),
    `content` TEXT,
    `broadcast_id` VARCHAR(100),
    `notification_type` VARCHAR(50),
    `is_read` TINYINT(1) DEFAULT 0,
    `created_at` DATETIME,
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `review` (
                                        `id` INT NOT NULL AUTO_INCREMENT,
                                        `shop_id` INT,
                                        `user_id` INT,
                                        `service_id` INT,
                                        `rating` INT,
                                        `comment` TEXT,
                                        `created_at` DATETIME,
                                        `reply` TEXT,
                                        `replied_at` DATETIME,
                                        PRIMARY KEY (`id`)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `membership_tier` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL UNIQUE,
    `required_spending` DOUBLE NOT NULL,
    `benefits` TEXT,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `invalidated_token` (
    `id` VARCHAR(255) NOT NULL,
    `expiry_time` DATETIME,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `user_token` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `token` VARCHAR(255) NOT NULL UNIQUE,
    `type` VARCHAR(50) NOT NULL,
    `user_id` INT NOT NULL,
    `expires_at` DATETIME NOT NULL,
    `used` TINYINT(1) DEFAULT 0,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `user_voucher` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `user_id` INT,
    `voucher_id` INT,
    `is_used` TINYINT(1) DEFAULT 0,
    `used_at` DATETIME,
    `expires_at` DATETIME,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`voucher_id`) REFERENCES `voucher`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `cage` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `shop_id` INT,
    `cage_code` VARCHAR(50),
    `type` VARCHAR(50),
    `is_available` TINYINT(1) DEFAULT 1,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`shop_id`) REFERENCES `shop`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `camera` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `cage_id` INT,
    `model_type` VARCHAR(100),
    `stream_url` VARCHAR(255),
    `access_token` VARCHAR(255),
    `status` VARCHAR(50),
    PRIMARY KEY (`id`),
    FOREIGN KEY (`cage_id`) REFERENCES `cage`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `boarding_detail` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `booking_id` INT,
    `cage_id` INT,
    `check_in` DATETIME,
    `check_out` DATETIME,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`booking_id`) REFERENCES `booking`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`cage_id`) REFERENCES `cage`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `booking_history` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `booking_id` INT,
    `old_status` VARCHAR(50),
    `new_status` VARCHAR(50),
    `changed_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `changed_by` VARCHAR(100),
    PRIMARY KEY (`id`),
    FOREIGN KEY (`booking_id`) REFERENCES `booking`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `pet_document` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `pet_id` INT,
    `type` VARCHAR(50),
    `title` VARCHAR(150),
    `content` TEXT,
    `image_url` VARCHAR(255),
    `record_date` DATE,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`pet_id`) REFERENCES `pet`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `pet_image` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `pet_id` INT,
    `image_url` VARCHAR(255),
    `description` TEXT,
    `upload_date` DATETIME,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`pet_id`) REFERENCES `pet`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `pet_meal` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `pet_id` INT,
    `meal_name` VARCHAR(100),
    `food_type` VARCHAR(100),
    `amount` VARCHAR(100),
    PRIMARY KEY (`id`),
    FOREIGN KEY (`pet_id`) REFERENCES `pet`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `pet_reminder` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `pet_id` INT,
    `title` VARCHAR(150),
    `description` TEXT,
    `date` DATETIME,
    `type` VARCHAR(50),
    `status` VARCHAR(50),
    PRIMARY KEY (`id`),
    FOREIGN KEY (`pet_id`) REFERENCES `pet`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `pet_vaccination` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `pet_id` INT,
    `booking_id` INT,
    `staff_id` INT,
    `name` VARCHAR(150),
    `drug` VARCHAR(150),
    `clinic` VARCHAR(150),
    `date` DATETIME,
    `status` VARCHAR(50),
    PRIMARY KEY (`id`),
    FOREIGN KEY (`pet_id`) REFERENCES `pet`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`booking_id`) REFERENCES `booking`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`staff_id`) REFERENCES `staff`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `staff_certificate` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `staff_id` INT,
    `certificate_name` VARCHAR(150),
    `image_url` TEXT,
    `issue_date` DATE,
    `expiry_date` DATE,
    `status` VARCHAR(50) DEFAULT 'PENDING',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`staff_id`) REFERENCES `staff`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `staff_change_request` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `booking_id` INT,
    `old_staff_id` INT,
    `proposed_staff_id` INT,
    `reason` TEXT NOT NULL,
    `status` VARCHAR(50) DEFAULT 'PENDING',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `processed_at` DATETIME,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`booking_id`) REFERENCES `booking`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`old_staff_id`) REFERENCES `staff`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`proposed_staff_id`) REFERENCES `staff`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `shop_wallet` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `shop_id` INT UNIQUE,
    `frozen_balance` DECIMAL(18,2) DEFAULT 0.00,
    `available_balance` DECIMAL(18,2) DEFAULT 0.00,
    `total_earned` DECIMAL(18,2) DEFAULT 0.00,
    `total_withdrawn` DECIMAL(18,2) DEFAULT 0.00,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`shop_id`) REFERENCES `shop`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `withdrawal_request` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `shop_id` INT,
    `amount` DECIMAL(18,2),
    `bank_name` VARCHAR(100),
    `bank_account` VARCHAR(50),
    `account_holder` VARCHAR(100),
    `note` TEXT,
    `status` VARCHAR(50) DEFAULT 'PENDING',
    `admin_note` TEXT,
    `payos_order_code` BIGINT,
    `checkout_url` VARCHAR(1000),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `processed_at` DATETIME,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`shop_id`) REFERENCES `shop`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `transaction` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `booking_id` INT,
    `shop_id` INT,
    `withdrawal_id` INT,
    `type` VARCHAR(30) NOT NULL,
    `amount` DECIMAL(18,2) NOT NULL,
    `payment_method` VARCHAR(20),
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `payos_order_code` BIGINT,
    `gateway_transaction_id` VARCHAR(100),
    `description` VARCHAR(255),
    `note` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `completed_at` DATETIME,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`booking_id`) REFERENCES `booking`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`shop_id`) REFERENCES `shop`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`withdrawal_id`) REFERENCES `withdrawal_request`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `message` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `shop_id` INT,
    `channel_type` VARCHAR(50) DEFAULT 'ADMIN_SUPPORT',
    `sender_email` VARCHAR(150),
    `recipient_email` VARCHAR(150),
    `target_id` BIGINT,
    `sender_role` VARCHAR(50),
    `content` TEXT,
    `attachment_url` VARCHAR(255),
    `attachment_type` VARCHAR(50),
    `attachment_name` VARCHAR(255),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `is_read` TINYINT(1) DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_message_shop_channel` (`shop_id`, `channel_type`),
    INDEX `idx_message_recipient` (`recipient_email`),
    INDEX `idx_message_sender` (`sender_email`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `shop_ai_chat_history` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `shop_id` INT NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_shop_ai_shop_id` (`shop_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `admin_ai_chat_history` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_admin_ai_user_id` (`user_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `ai_gateway_messages` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `session_id` VARCHAR(255) NOT NULL,
    `agent_type` VARCHAR(50) NOT NULL,
    `owner_key` VARCHAR(255) NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `content` TEXT NOT NULL,
    `tool_result_json` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_ai_msg_session` (`session_id`, `created_at`),
    INDEX `idx_ai_msg_owner` (`owner_key`, `agent_type`)
) ENGINE=InnoDB;


-- ============================================================================
-- PHẦN 3: INSERT SEED DATA
-- Lưu ý: Tên bảng `user` bọc trong kí tự `` để tránh trùng từ khóa hệ thống!
-- ============================================================================

-- 1. ROLES
INSERT INTO role (id, name, description) VALUES
(1, 'USER',       'Khách hàng đặt dịch vụ thú cưng'),
(2, 'SHOP_OWNER', 'Chủ cửa hàng thú cưng'),
(3, 'ADMIN',      'Quản trị viên hệ thống'),
(4, 'STAFF',      'Nhân viên cửa hàng thú cưng');

-- 2. USERS (password: 123456)
INSERT INTO `user` (id, email, password, full_name, phone, address, avatar, created_at, active, email_verified, just_upgraded, tier_id, total_spending) VALUES
(1, 'customer1@gmail.com', '$2a$10$9b0cM.sL3rnwfnjCMLqtYuBsEAwwSTympZuvd9OuP5EylVY6Gnqe2', 'Nguyễn Văn An',          '0901234567', '123 Nguyễn Huệ, Q1, TP.HCM',              NULL, '2024-01-15 10:00:00', 1, 1, 0, NULL, 1080000),
(2, 'customer2@gmail.com', '$2a$10$9b0cM.sL3rnwfnjCMLqtYuBsEAwwSTympZuvd9OuP5EylVY6Gnqe2', 'Trần Thị Bình',           '0902345678', '456 Lê Lợi, Q1, TP.HCM',                  NULL, '2024-02-20 11:00:00', 1, 1, 0, NULL, 450000),
(3, 'customer3@gmail.com', '$2a$10$9b0cM.sL3rnwfnjCMLqtYuBsEAwwSTympZuvd9OuP5EylVY6Gnqe2', 'Lê Văn Cường',            '0903456789', '789 Trần Hưng Đạo, Q5, TP.HCM',            NULL, '2024-03-10 12:00:00', 1, 1, 0, NULL, 290000),
(4, 'customer4@gmail.com', '$2a$10$9b0cM.sL3rnwfnjCMLqtYuBsEAwwSTympZuvd9OuP5EylVY6Gnqe2', 'Phạm Thị Dung',           '0904567890', '321 Võ Văn Tần, Q3, TP.HCM',               NULL, '2024-04-05 13:00:00', 1, 1, 0, NULL, 400000),
(5, 'customer5@gmail.com', '$2a$10$9b0cM.sL3rnwfnjCMLqtYuBsEAwwSTympZuvd9OuP5EylVY6Gnqe2', 'Hoàng Văn Em',            '0905678901', '654 Cách Mạng Tháng 8, Q10, TP.HCM',       NULL, '2024-05-12 14:00:00', 1, 1, 0, NULL, 1550000),
(6, 'shop1@gmail.com',     '$2a$10$9b0cM.sL3rnwfnjCMLqtYuBsEAwwSTympZuvd9OuP5EylVY6Gnqe2', 'Dr. Minh Pet Clinic Owner','0906789012', '100 Điện Biên Phủ, Q3, TP.HCM',            NULL, '2023-10-01 08:00:00', 1, 1, 0, NULL, 0),
(7, 'shop2@gmail.com',     '$2a$10$9b0cM.sL3rnwfnjCMLqtYuBsEAwwSTympZuvd9OuP5EylVY6Gnqe2', 'Happy Paws Spa Owner',    '0907890123', '200 Nguyễn Thị Minh Khai, Q1, TP.HCM',     NULL, '2023-11-15 09:00:00', 1, 1, 0, NULL, 0),
(8, 'shop3@gmail.com',     '$2a$10$9b0cM.sL3rnwfnjCMLqtYuBsEAwwSTympZuvd9OuP5EylVY6Gnqe2', 'Pet Paradise Hotel Owner','0908901234', '300 Lý Thường Kiệt, Q10, TP.HCM',          NULL, '2023-12-01 10:00:00', 1, 1, 0, NULL, 0);

-- User-Role
INSERT INTO user_roles (user_id, roles_id) VALUES
(1, 1), (2, 1), (3, 1), (4, 1), (5, 1),
(6, 2), (7, 2), (8, 2);

-- 3. SHOPS
INSERT INTO shop (id, owner_id, shop_name, shop_type, email, phone, address, city, latitude, longitude, description, license_number, license_image_url, logo_url, banner_url, gallery_urls, open_time, close_time, working_days, rating_avg, is_verified, status, assignment_mode, late_grace_period) VALUES
(1, 6, 'Dr. Minh Pet Clinic',       'CLINIC',   'shop1@gmail.com', '0906789012', '100 Điện Biên Phủ, Phường 25, Quận Bình Thạnh, TP.HCM', 'TP.HCM', 10.8050514, 106.7128282, 'Phòng khám thú y uy tín với đội ngũ bác sĩ giàu kinh nghiệm, chuyên khám và điều trị bệnh cho chó mèo.',    'LIC2023001', NULL, NULL, NULL, NULL, '08:00', '20:00', 'MON,TUE,WED,THU,FRI,SAT,SUN', 4.8, 1, 'APPROVED', 'MANUAL',    15),
(2, 7, 'Happy Paws Spa & Grooming', 'SPA',      'shop2@gmail.com', '0907890123', '200 Nguyễn Thị Minh Khai, Phường 6, Quận 3, TP.HCM',     'TP.HCM', 10.7861851, 106.6886789, 'Spa và grooming chuyên nghiệp cho thú cưng, dịch vụ tắm sấy, cắt tỉa lông, chăm sóc móng tai.',            'LIC2023002', NULL, NULL, NULL, NULL, '09:00', '21:00', 'MON,TUE,WED,THU,FRI,SAT,SUN', 4.6, 1, 'APPROVED', 'OPEN_POOL', 15),
(3, 8, 'Pet Paradise Hotel',        'BOARDING', 'shop3@gmail.com', '0908901234', '300 Lý Thường Kiệt, Phường 14, Quận 10, TP.HCM',          'TP.HCM', 10.7721885, 106.6677386, 'Khách sạn thú cưng 5 sao, phòng rộng rãi có điều hòa, camera giám sát 24/7, nhân viên chăm sóc tận tình.', 'LIC2023003', NULL, NULL, NULL, NULL, '00:00', '23:59', 'MON,TUE,WED,THU,FRI,SAT,SUN', 4.9, 1, 'APPROVED', 'AUTO',      15);

-- 4. STAFF
INSERT INTO staff (id, shop_id, user_id, full_name, role, phone, specialization, is_active) VALUES
-- Shop 1 (Clinic)
(1,  1, NULL, 'Bác sĩ Nguyễn Văn A', 'Veterinarian',       '0911111111', 'Khám và điều trị nội khoa thú cưng',    1),
(2,  1, NULL, 'Bác sĩ Trần Thị B',   'Veterinarian',       '0911111112', 'Da liễu và dị ứng thú cưng',            1),
(3,  1, NULL, 'Lê Văn C',            'Vet Assistant',      '0911111113', 'Hỗ trợ phẫu thuật và chăm sóc hậu phẫu',1),
(4,  1, NULL, 'Phạm Thị D',          'Receptionist',       '0911111114', 'Lễ tân và tư vấn dịch vụ thú cưng',     1),
(5,  1, NULL, 'Hoàng Văn E',         'Lab Technician',     '0911111115', 'Xét nghiệm và chẩn đoán hình ảnh',      1),
(6,  1, NULL, 'Ngô Thị F',           'Pet Nurse',          '0911111116', 'Chăm sóc và theo dõi thú cưng nội trú', 1),
(7,  1, NULL, 'Vũ Văn G',            'Pharmacist',         '0911111117', 'Quản lý thuốc thú y và kê đơn',         1),
-- Shop 2 (Spa)
(8,  2, NULL, 'Đỗ Thị H',            'Head Groomer',       '0922222221', 'Cắt tỉa tạo kiểu lông chuyên nghiệp',  1),
(9,  2, NULL, 'Bùi Văn I',           'Groomer',            '0922222222', 'Grooming và tạo kiểu cho chó mèo',      1),
(10, 2, NULL, 'Lý Thị K',            'Groomer',            '0922222223', 'Spa và massage thư giãn cho thú cưng',  1),
(11, 2, NULL, 'Mai Văn L',           'Bath Specialist',    '0922222224', 'Tắm và vệ sinh toàn thân thú cưng',     1),
(12, 2, NULL, 'Trịnh Thị M',         'Receptionist',       '0922222225', 'Lễ tân và đặt lịch dịch vụ',            1),
(13, 2, NULL, 'Võ Văn N',            'Grooming Assistant', '0922222226', 'Hỗ trợ grooming và chăm sóc',           1),
(14, 2, NULL, 'Hồ Thị O',            'Nail Technician',    '0922222227', 'Cắt móng và chăm sóc bàn chân thú cưng',1),
-- Shop 3 (Hotel)
(15, 3, NULL, 'Đặng Văn P',          'Hotel Manager',      '0933333331', 'Quản lý khách sạn thú cưng',            1),
(16, 3, NULL, 'Phan Thị Q',          'Pet Caretaker',      '0933333332', 'Chăm sóc thú cưng lưu trú',             1),
(17, 3, NULL, 'Dương Văn R',         'Pet Caretaker',      '0933333333', 'Cho ăn uống và vệ sinh thú cưng',       1),
(18, 3, NULL, 'Tô Thị S',            'Activity Trainer',   '0933333334', 'Tổ chức hoạt động vui chơi cho thú cưng',1),
(19, 3, NULL, 'Lưu Văn T',           'Night Caretaker',    '0933333335', 'Trông nom thú cưng ca đêm',              1),
(20, 3, NULL, 'Chu Thị U',           'Receptionist',       '0933333336', 'Lễ tân check-in/out thú cưng',           1);

-- 5. PETS
INSERT INTO pet (id, owner_id, name, species, breed, gender, color, avatar, sterilized, weight, dob, health_note, favorite_food, allergies, hobbies, walk_time, is_active, unactive_reason) VALUES
(1, 1, 'Lucky',   'Dog', 'Golden Retriever',  'Male',   'Golden',         NULL, 1, 28.5, '2020-05-15', 'Khỏe mạnh, đã tiêm phòng đầy đủ',  'Thịt gà, cơm',       'Không',   'Chơi bóng, bơi lội',   '06:00-07:00, 18:00-19:00', 1, NULL),
(2, 2, 'Miu Miu', 'Cat', 'British Shorthair', 'Female', 'Xám xanh',       NULL, 1, 4.2,  '2021-03-20', 'Khỏe mạnh',                         'Pate cá ngừ',        'Sữa bò',  'Ngủ, leo trèo',        NULL,                       1, NULL),
(3, 3, 'Max',     'Dog', 'Husky',             'Male',   'Đen và trắng',   NULL, 0, 25.0, '2019-11-10', 'Khỏe mạnh, rất năng động',          'Thức ăn khô, thịt bò','Không',  'Chạy bộ, kéo xe',     '05:30-06:30, 17:00-18:00', 1, NULL),
(4, 4, 'Bella',   'Dog', 'Poodle',            'Female', 'Trắng',          NULL, 1, 5.8,  '2022-01-08', 'Khỏe mạnh, thân thiện',             'Thức ăn mềm',        'Không',   'Chơi đùa, được chiều', '07:00-07:30, 19:00-19:30', 1, NULL),
(5, 5, 'Simba',   'Cat', 'Persian',           'Male',   'Cam vàng',       NULL, 1, 4.8,  '2020-08-25', 'Khỏe mạnh',                         'Pate gà, cá hồi',    'Hải sản', 'Nằm phơi nắng',        NULL,                       1, NULL),
(6, 1, 'Rocky',   'Dog', 'Bulldog',           'Male',   'Nâu',            NULL, 0, 22.0, '2021-06-12', 'Khỏe mạnh',                         'Thịt heo, rau củ',   'Không',   'Ăn và ngủ',            '06:30-07:00',              1, NULL),
(7, 3, 'Luna',    'Cat', 'Siamese',           'Female', 'Kem và nâu',     NULL, 1, 3.5,  '2022-09-05', 'Khỏe mạnh, hiếu động',              'Pate cá, thịt gà',   'Không',   'Leo trèo, săn đồ chơi',NULL,                       1, NULL);

-- 6. SERVICES (tên gọi thân thiện với thú cưng)
INSERT INTO pet_service (id, shop_id, service_name, category, price, duration_minutes, description, image_url, active, created_at, camera_enabled, cage_size, room_type, camera_tiers, camera_tier_prices, camera_tier_labels, camera_description) VALUES
-- Clinic (shop 1)
(1,  1, 'Khám sức khỏe tổng quát',     'CLINIC',   200000,  30,   'Kiểm tra toàn diện sức khỏe thú cưng: tim, phổi, răng, da, cân nặng', NULL, 1, '2024-01-01 10:00:00', 0, NULL, NULL, NULL, NULL, NULL, NULL),
(2,  1, 'Tiêm phòng thú cưng',          'CLINIC',   150000,  15,   'Tiêm vaccine phòng dại, parvovirus, carré và các bệnh truyền nhiễm',   NULL, 1, '2024-01-01 10:00:00', 0, NULL, NULL, NULL, NULL, NULL, NULL),
(3,  1, 'Phẫu thuật triệt sản',         'CLINIC',  1500000, 120,   'Phẫu thuật triệt sản an toàn cho chó mèo, gây mê toàn thân',          NULL, 1, '2024-01-01 10:00:00', 0, NULL, NULL, NULL, NULL, NULL, NULL),
(4,  1, 'Xét nghiệm máu thú cưng',      'CLINIC',   300000,  20,   'Xét nghiệm máu toàn phần, kiểm tra chức năng gan, thận, tuyến giáp',  NULL, 1, '2024-01-01 10:00:00', 0, NULL, NULL, NULL, NULL, NULL, NULL),
(5,  1, 'Siêu âm chẩn đoán',            'CLINIC',   400000,  30,   'Siêu âm bụng chẩn đoán các bệnh lý nội tạng thú cưng',                NULL, 1, '2024-01-01 10:00:00', 0, NULL, NULL, NULL, NULL, NULL, NULL),
-- Spa (shop 2)
(6,  2, 'Tắm sấy cơ bản',               'SPA',      150000,  45,   'Tắm bằng sữa tắm chuyên dụng cho thú cưng, sấy khô và chải lông',    NULL, 1, '2024-01-01 10:00:00', 0, NULL, NULL, NULL, NULL, NULL, NULL),
(7,  2, 'Tắm spa cao cấp & Massage',    'SPA',      300000,  60,   'Tắm dưỡng lông kèm massage thư giãn, mang lại cảm giác dễ chịu',     NULL, 1, '2024-01-01 10:00:00', 0, NULL, NULL, NULL, NULL, NULL, NULL),
(8,  2, 'Cắt tỉa lông cơ bản',          'GROOMING', 200000,  60,   'Cắt tỉa gọn gàng theo form chuẩn của giống, vệ sinh tai và mắt',     NULL, 1, '2024-01-01 10:00:00', 0, NULL, NULL, NULL, NULL, NULL, NULL),
(9,  2, 'Tạo kiểu lông chuyên nghiệp',  'GROOMING', 400000,  90,   'Tạo kiểu lông theo yêu cầu, tỉa tạo hình đẹp cho chó mèo',           NULL, 1, '2024-01-01 10:00:00', 0, NULL, NULL, NULL, NULL, NULL, NULL),
(10, 2, 'Cắt móng & Vệ sinh tai',       'SPA',      100000,  30,   'Cắt móng an toàn, vệ sinh tai và tuyến hôi cho thú cưng',            NULL, 1, '2024-01-01 10:00:00', 0, NULL, NULL, NULL, NULL, NULL, NULL),
-- Hotel/Boarding (shop 3)
(11, 3, 'Lưu trú phòng Standard',       'BOARDING', 200000, 1440,  'Phòng tiêu chuẩn cho chó mèo, sạch sẽ thoáng mát, có camera giám sát', NULL, 1, '2024-01-01 10:00:00', 1, 'MEDIUM',      'STANDARD', '["BASIC","HD"]', '{"BASIC":0,"HD":50000}',    '{"BASIC":"Camera thường","HD":"Camera HD"}',  'Camera theo dõi thú cưng 24/7'),
(12, 3, 'Lưu trú phòng VIP',            'BOARDING', 400000, 1440,  'Phòng VIP rộng rãi, có điều hòa và đồ chơi, camera HD giám sát',     NULL, 1, '2024-01-01 10:00:00', 1, 'LARGE',       'VIP',      '["HD","AI"]',   '{"HD":0,"AI":100000}',      '{"HD":"Camera HD","AI":"Camera AI"}',         'Camera AI phát hiện hành vi bất thường'),
(13, 3, 'Lưu trú phòng Suite',          'BOARDING', 600000, 1440,  'Phòng suite sang trọng, không gian rộng lớn, camera AI thông minh',   NULL, 1, '2024-01-01 10:00:00', 1, 'EXTRA_LARGE', 'SUITE',    '["AI"]',        '{"AI":0}',                  '{"AI":"Camera AI Pro"}',                      'Camera AI Pro phát hiện và cảnh báo hành vi'),
(14, 3, 'Đưa đón thú cưng tận nhà',     'TRANSPORT',150000,  60,   'Dịch vụ đưa đón thú cưng tận nơi trong khu vực TP.HCM',              NULL, 1, '2024-01-01 10:00:00', 0, NULL, NULL, NULL, NULL, NULL, NULL),
(15, 3, 'Chăm sóc đặc biệt thú cưng',  'CARE',     100000,  30,   'Chăm sóc riêng cho thú cưng già, bệnh hoặc có nhu cầu đặc biệt',     NULL, 1, '2024-01-01 10:00:00', 0, NULL, NULL, NULL, NULL, NULL, NULL);

-- 7. VOUCHERS
INSERT INTO voucher (id, code, discount_type, discount_value, min_order_value, max_discount_amount, valid_days, issue_quantity, target_tier_id) VALUES
(1, 'WELCOME10',  'PERCENTAGE',  10, 100000,  50000, 30, 1, NULL),
(2, 'SAVE50K',    'FIXED_AMOUNT',50000, 200000, NULL, 30, 1, NULL),
(3, 'GROOMING20', 'PERCENTAGE',  20, 150000, 100000, 30, 1, NULL),
(4, 'NEWPET15',   'PERCENTAGE',  15,      0,  30000, 60, 1, NULL),
(5, 'VIP100K',    'FIXED_AMOUNT',100000, 500000, NULL,30, 1, NULL);

-- 8. BOOKINGS
-- customer1 (id=1): booking 1,5,12 COMPLETED + booking 11 CONFIRMED → có lịch sử + lịch sắp tới
-- customer2 (id=2): booking 2 COMPLETED + booking 8 CONFIRMED
-- customer3 (id=3): booking 3 COMPLETED + booking 7 CONFIRMED
-- customer4 (id=4): booking 4 COMPLETED + booking 9 CONFIRMED
-- customer5 (id=5): booking 6 COMPLETED + booking 10 CANCELLED
INSERT INTO booking (id, user_id, shop_id, pet_id, staff_id, appointment_datetime, check_out_datetime, service_start_datetime, service_end_datetime, cage_size, room_type, status, note, voucher_id, discount_amount, cancellation_reason, bank_name, bank_account, account_holder, rtsp_link, completed_service_ids, completed_service_times, payos_order_code, camera_rtsp_url, camera_stream_url, camera_configured_at, check_in, check_out, created_at, updated_at) VALUES
(1,  1, 1, 1,  1, '2024-05-10 10:00:00', NULL,                 '2024-05-10 10:00:00', '2024-05-10 10:30:00', NULL,    NULL,       'COMPLETED', 'Khám sức khỏe định kỳ cho Lucky',      1, 20000, NULL, NULL, NULL, NULL, NULL, '1',   '2024-05-10 10:30:00', 1001, NULL, NULL, NULL, '2024-05-10 10:00:00', '2024-05-10 10:30:00', '2024-05-08 15:00:00', '2024-05-10 10:30:00'),
(2,  2, 1, 2,  2, '2024-05-12 14:00:00', NULL,                 '2024-05-12 14:00:00', '2024-05-12 14:15:00', NULL,    NULL,       'COMPLETED', 'Tiêm phòng định kỳ cho Miu Miu',       NULL, 0,    NULL, NULL, NULL, NULL, NULL, '2',   '2024-05-12 14:15:00', 1002, NULL, NULL, NULL, '2024-05-12 14:00:00', '2024-05-12 14:15:00', '2024-05-10 09:00:00', '2024-05-12 14:15:00'),
(3,  3, 2, 3,  8, '2024-05-15 09:00:00', NULL,                 '2024-05-15 09:00:00', '2024-05-15 10:30:00', NULL,    NULL,       'COMPLETED', 'Tắm sấy và cắt tỉa lông cho Max',      3, 60000, NULL, NULL, NULL, NULL, NULL, '6,8', '2024-05-15 10:30:00', 1003, NULL, NULL, NULL, '2024-05-15 09:00:00', '2024-05-15 10:30:00', '2024-05-13 11:00:00', '2024-05-15 10:30:00'),
(4,  4, 2, 4,  9, '2024-05-18 10:00:00', NULL,                 '2024-05-18 10:00:00', '2024-05-18 11:30:00', NULL,    NULL,       'COMPLETED', 'Tạo kiểu lông chuyên nghiệp cho Bella',NULL, 0,    NULL, NULL, NULL, NULL, NULL, '9',   '2024-05-18 11:30:00', 1004, NULL, NULL, NULL, '2024-05-18 10:00:00', '2024-05-18 11:30:00', '2024-05-16 14:00:00', '2024-05-18 11:30:00'),
(5,  1, 3, 6, 16, '2024-05-25 08:00:00', '2024-05-28 18:00:00','2024-05-25 08:00:00', '2024-05-28 18:00:00','MEDIUM', 'STANDARD', 'COMPLETED', 'Gửi Rocky lưu trú 3 ngày',             NULL, 0,    NULL, NULL, NULL, NULL, NULL, '11',  '2024-05-28 18:00:00', 1005, NULL, NULL, NULL, '2024-05-25 08:00:00', '2024-05-28 18:00:00', '2024-05-22 10:00:00', '2024-05-28 18:00:00'),
(6,  5, 3, 5, 17, '2024-05-30 10:00:00', '2024-06-03 10:00:00','2024-05-30 10:00:00', '2024-06-03 10:00:00','LARGE',  'VIP',      'COMPLETED', 'Gửi Simba lưu trú 4 ngày phòng VIP',   2, 50000, NULL, NULL, NULL, NULL, NULL, '12',  '2024-06-03 10:00:00', 1006, NULL, NULL, NULL, '2024-05-30 10:00:00', '2024-06-03 10:00:00', '2024-05-27 16:00:00', '2024-06-03 10:00:00'),
(7,  3, 1, 7,  1, '2026-06-20 15:00:00', NULL,                 NULL,                  NULL,                  NULL,    NULL,       'CONFIRMED', 'Khám sức khỏe tổng quát cho Luna',     NULL, 0,    NULL, NULL, NULL, NULL, NULL, NULL,  NULL,                  1007, NULL, NULL, NULL, NULL,                 NULL,                 '2026-06-04 09:00:00', '2026-06-04 09:00:00'),
(8,  2, 2, 2, 10, '2026-06-22 11:00:00', NULL,                 NULL,                  NULL,                  NULL,    NULL,       'CONFIRMED', 'Tắm spa cao cấp cho Miu Miu',          NULL, 0,    NULL, NULL, NULL, NULL, NULL, NULL,  NULL,                  1008, NULL, NULL, NULL, NULL,                 NULL,                 '2026-06-04 14:00:00', '2026-06-04 14:00:00'),
(9,  4, 3, 4, 18, '2026-06-25 09:00:00', '2026-06-30 09:00:00',NULL,                  NULL,                  'EXTRA_LARGE','SUITE','CONFIRMED','Gửi Bella 5 ngày phòng Suite',         NULL, 0,    NULL, NULL, NULL, NULL, NULL, NULL,  NULL,                  1009, NULL, NULL, NULL, NULL,                 NULL,                 '2026-06-04 17:00:00', '2026-06-04 17:00:00'),
(10, 5, 1, 5,  3, '2024-05-20 10:00:00', NULL,                 NULL,                  NULL,                  NULL,    NULL,       'CANCELLED', 'Phẫu thuật triệt sản cho Simba',       NULL, 0,    'Khách hàng đổi lịch', NULL, NULL, NULL, NULL, NULL, NULL, 1010, NULL, NULL, NULL, NULL, NULL, '2024-05-18 08:00:00', '2024-05-19 10:00:00'),
(11, 1, 2, 1,  8, '2026-06-28 10:00:00', NULL,                 NULL,                  NULL,                  NULL,    NULL,       'CONFIRMED', 'Tắm sấy và làm đẹp cho Lucky',         NULL, 0,    NULL, NULL, NULL, NULL, NULL, NULL,  NULL,                  1011, NULL, NULL, NULL, NULL,                 NULL,                 '2026-06-04 10:00:00', '2026-06-04 10:00:00'),
(12, 1, 1, 6,  4, '2024-04-15 09:00:00', NULL,                 '2024-04-15 09:00:00', '2024-04-15 09:30:00', NULL,    NULL,       'COMPLETED', 'Tiêm phòng cho Rocky',                 NULL, 0,    NULL, NULL, NULL, NULL, NULL, '2',   '2024-04-15 09:30:00', 1012, NULL, NULL, NULL, '2024-04-15 09:00:00', '2024-04-15 09:30:00', '2024-04-13 08:00:00', '2024-04-15 09:30:00');

-- Booking-Services
INSERT INTO booking_services (booking_id, service_id) VALUES
(1, 1), (2, 2), (3, 6), (3, 8), (4, 9),
(5, 11), (6, 12), (7, 1), (8, 7), (9, 13), (10, 3),
(11, 6), (12, 2);

-- 9. PAYMENTS (đồng bộ với bookings COMPLETED + CONFIRMED đã thanh toán)
INSERT INTO payment (id, booking_id, amount, method, status, payos_order_code, checkout_url, gateway_transaction_id, payment_time, description) VALUES
(1,  1,  180000,  'PAYOS', 'SUCCESS', 1001, 'https://payos.vn/checkout/1001', 'TXN1001', '2024-05-08 15:30:00', 'Thanh toán khám sức khỏe tổng quát cho Lucky'),
(2,  2,  150000,  'PAYOS', 'SUCCESS', 1002, 'https://payos.vn/checkout/1002', 'TXN1002', '2024-05-10 09:15:00', 'Thanh toán tiêm phòng cho Miu Miu'),
(3,  3,  290000,  'PAYOS', 'SUCCESS', 1003, 'https://payos.vn/checkout/1003', 'TXN1003', '2024-05-13 11:20:00', 'Thanh toán tắm sấy và cắt tỉa lông cho Max'),
(4,  4,  400000,  'PAYOS', 'SUCCESS', 1004, 'https://payos.vn/checkout/1004', 'TXN1004', '2024-05-16 14:10:00', 'Thanh toán tạo kiểu lông cho Bella'),
(5,  5,  600000,  'PAYOS', 'SUCCESS', 1005, 'https://payos.vn/checkout/1005', 'TXN1005', '2024-05-22 10:25:00', 'Thanh toán lưu trú 3 ngày phòng Standard cho Rocky'),
(6,  6,  1550000, 'PAYOS', 'SUCCESS', 1006, 'https://payos.vn/checkout/1006', 'TXN1006', '2024-05-27 16:30:00', 'Thanh toán lưu trú 4 ngày phòng VIP cho Simba'),
(7,  7,  200000,  'PAYOS', 'SUCCESS', 1007, 'https://payos.vn/checkout/1007', 'TXN1007', '2026-06-04 09:10:00', 'Thanh toán khám sức khỏe cho Luna'),
(8,  8,  300000,  'PAYOS', 'SUCCESS', 1008, 'https://payos.vn/checkout/1008', 'TXN1008', '2026-06-04 14:15:00', 'Thanh toán tắm spa cho Miu Miu'),
(9,  9,  3000000, 'PAYOS', 'SUCCESS', 1009, 'https://payos.vn/checkout/1009', 'TXN1009', '2026-06-04 17:05:00', 'Thanh toán lưu trú Suite 5 ngày cho Bella'),
(10, 11, 150000,  'PAYOS', 'SUCCESS', 1011, 'https://payos.vn/checkout/1011', 'TXN1011', '2026-06-04 10:10:00', 'Thanh toán tắm sấy và làm đẹp cho Lucky'),
(11, 12, 150000,  'PAYOS', 'SUCCESS', 1012, 'https://payos.vn/checkout/1012', 'TXN1012', '2024-04-13 08:20:00', 'Thanh toán tiêm phòng cho Rocky');

-- 10. TRANSACTIONS (đồng bộ payment + wallet credit 90% cho shop)
INSERT INTO `transaction` (id, booking_id, shop_id, withdrawal_id, type, amount, payment_method, status, payos_order_code, gateway_transaction_id, description, note, created_at, completed_at) VALUES
(1,  1,  1, NULL, 'BOOKING_PAYMENT', 180000.00,  'PAYOS', 'SUCCESS', 1001, 'TXN1001', 'Khách thanh toán booking #1 - Lucky khám sức khỏe',    NULL, '2024-05-08 15:30:00', '2024-05-08 15:30:00'),
(2,  1,  1, NULL, 'WALLET_CREDIT',   162000.00,  'PAYOS', 'SUCCESS', 1001, 'TXN1001', 'Shop nhận 90% booking #1',                              NULL, '2024-05-10 10:30:00', '2024-05-10 10:30:00'),
(3,  2,  1, NULL, 'BOOKING_PAYMENT', 150000.00,  'PAYOS', 'SUCCESS', 1002, 'TXN1002', 'Khách thanh toán booking #2 - Miu Miu tiêm phòng',      NULL, '2024-05-10 09:15:00', '2024-05-10 09:15:00'),
(4,  2,  1, NULL, 'WALLET_CREDIT',   135000.00,  'PAYOS', 'SUCCESS', 1002, 'TXN1002', 'Shop nhận 90% booking #2',                              NULL, '2024-05-12 14:15:00', '2024-05-12 14:15:00'),
(5,  3,  2, NULL, 'BOOKING_PAYMENT', 290000.00,  'PAYOS', 'SUCCESS', 1003, 'TXN1003', 'Khách thanh toán booking #3 - Max tắm và cắt tỉa lông', NULL, '2024-05-13 11:20:00', '2024-05-13 11:20:00'),
(6,  3,  2, NULL, 'WALLET_CREDIT',   261000.00,  'PAYOS', 'SUCCESS', 1003, 'TXN1003', 'Shop nhận 90% booking #3',                              NULL, '2024-05-15 10:30:00', '2024-05-15 10:30:00'),
(7,  4,  2, NULL, 'BOOKING_PAYMENT', 400000.00,  'PAYOS', 'SUCCESS', 1004, 'TXN1004', 'Khách thanh toán booking #4 - Bella tạo kiểu lông',     NULL, '2024-05-16 14:10:00', '2024-05-16 14:10:00'),
(8,  4,  2, NULL, 'WALLET_CREDIT',   360000.00,  'PAYOS', 'SUCCESS', 1004, 'TXN1004', 'Shop nhận 90% booking #4',                              NULL, '2024-05-18 11:30:00', '2024-05-18 11:30:00'),
(9,  5,  3, NULL, 'BOOKING_PAYMENT', 600000.00,  'PAYOS', 'SUCCESS', 1005, 'TXN1005', 'Khách thanh toán booking #5 - Rocky lưu trú 3 ngày',    NULL, '2024-05-22 10:25:00', '2024-05-22 10:25:00'),
(10, 5,  3, NULL, 'WALLET_CREDIT',   540000.00,  'PAYOS', 'SUCCESS', 1005, 'TXN1005', 'Shop nhận 90% booking #5',                              NULL, '2024-05-28 18:00:00', '2024-05-28 18:00:00'),
(11, 6,  3, NULL, 'BOOKING_PAYMENT', 1550000.00, 'PAYOS', 'SUCCESS', 1006, 'TXN1006', 'Khách thanh toán booking #6 - Simba lưu trú 4 ngày',    NULL, '2024-05-27 16:30:00', '2024-05-27 16:30:00'),
(12, 6,  3, NULL, 'WALLET_CREDIT',   1395000.00, 'PAYOS', 'SUCCESS', 1006, 'TXN1006', 'Shop nhận 90% booking #6',                              NULL, '2024-06-03 10:00:00', '2024-06-03 10:00:00'),
(13, 12, 1, NULL, 'BOOKING_PAYMENT', 150000.00,  'PAYOS', 'SUCCESS', 1012, 'TXN1012', 'Khách thanh toán booking #12 - Rocky tiêm phòng',       NULL, '2024-04-13 08:20:00', '2024-04-13 08:20:00'),
(14, 12, 1, NULL, 'WALLET_CREDIT',   135000.00,  'PAYOS', 'SUCCESS', 1012, 'TXN1012', 'Shop nhận 90% booking #12',                             NULL, '2024-04-15 09:30:00', '2024-04-15 09:30:00');

-- 11. SHOP WALLETS
-- Shop 1: 162000 + 135000 + 135000 = 432000
-- Shop 2: 261000 + 360000 = 621000
-- Shop 3: 540000 + 1395000 = 1935000
INSERT INTO shop_wallet (id, shop_id, frozen_balance, available_balance, total_earned, total_withdrawn, updated_at) VALUES
(1, 1, 0.00,    432000.00,  432000.00,  0.00, '2024-05-12 14:15:00'),
(2, 2, 0.00,    621000.00,  621000.00,  0.00, '2024-05-18 11:30:00'),
(3, 3, 0.00,   1935000.00, 1935000.00,  0.00, '2024-06-03 10:00:00');

-- 12. PET MEDICAL RECORDS
INSERT INTO pet_medical_record (id, pet_id, booking_id, staff_id, diagnosis, treatment, prescription, visit_date, veterinarian_note) VALUES
(1, 1, 1,  1, 'Sức khỏe tốt, không phát hiện bất thường',  'Không cần điều trị',        'Vitamin tổng hợp dành cho chó 1 viên/ngày',             '2024-05-10 10:00:00', 'Lucky rất khỏe mạnh, tiếp tục chế độ dinh dưỡng hiện tại'),
(2, 2, 2,  2, 'Khỏe mạnh, tiêm phòng đầy đủ',              'Tiêm vaccine 5 bệnh mèo',   'Không cần thuốc thêm, hẹn tái tiêm sau 12 tháng',      '2024-05-12 14:00:00', 'Miu Miu rất ngoan, không có phản ứng sau tiêm'),
(3, 6, 12, 4, 'Khỏe mạnh, cập nhật lịch tiêm phòng',       'Tiêm phòng dại mũi 2',      'Không cần thuốc',                                        '2024-04-15 09:00:00', 'Rocky hoàn thành lịch tiêm phòng, hẹn năm sau'),
(4, 7, 7,  1, 'Thiếu cân nhẹ, cần tăng lượng thức ăn',     'Tư vấn dinh dưỡng',         'Thức ăn giàu protein dành cho mèo, 3 bữa/ngày',         '2026-06-20 15:00:00', 'Luna cần tăng thêm 0.5kg, theo dõi sau 1 tháng');

-- 13. CARE LOGS
INSERT INTO care_log (id, booking_id, staff_id, type, note, timestamp, image_url) VALUES
(1,  5, 16, 'FEEDING',  'Rocky ăn sáng ngon miệng, ăn hết 200g thức ăn khô',       '2024-05-25 08:30:00', NULL),
(2,  5, 16, 'CLEANING', 'Vệ sinh chuồng và thay đệm ngủ cho Rocky',                '2024-05-25 10:00:00', NULL),
(3,  5, 17, 'EXERCISE', 'Rocky đi dạo 30 phút và chơi bóng trong sân chơi',        '2024-05-25 16:00:00', NULL),
(4,  5, 16, 'FEEDING',  'Rocky ăn tối, ăn hết phần, đang vui vẻ',                  '2024-05-25 18:00:00', NULL),
(5,  5, 19, 'MEDICAL',  'Kiểm tra sức khỏe Rocky: nhiệt độ bình thường, ăn uống tốt','2024-05-26 08:00:00', NULL),
(6,  5, 16, 'FEEDING',  'Rocky ăn sáng ngày 2, rất ngoan và vui vẻ',               '2024-05-26 08:30:00', NULL),
(7,  6, 17, 'FEEDING',  'Simba ăn sáng hết phần pate cá hồi',                      '2024-05-30 10:30:00', NULL),
(8,  6, 17, 'CLEANING', 'Vệ sinh phòng VIP, thay khăn và đệm nằm cho Simba',       '2024-05-30 11:00:00', NULL),
(9,  6, 18, 'EXERCISE', 'Simba chơi với đồ chơi mèo và cần câu 20 phút',           '2024-05-30 15:00:00', NULL),
(10, 6, 17, 'FEEDING',  'Simba ăn trưa, thích pate gà hơn cá',                     '2024-05-31 12:30:00', NULL);

-- 14. REVIEWS
INSERT INTO review (id, shop_id, user_id, service_id, rating, comment, created_at, reply, replied_at) VALUES
(1, 1, 1, 1,  5, 'Bác sĩ rất tận tình, Lucky được khám rất kỹ từ đầu đến chân. Phòng khám sạch sẽ, chuyên nghiệp!',      '2024-05-10 12:00:00', 'Cảm ơn anh An đã tin tưởng Dr. Minh Pet Clinic. Chúc Lucky luôn mạnh khỏe!',          '2024-05-10 15:00:00'),
(2, 1, 2, 2,  5, 'Tiêm phòng nhanh gọn, Miu không sợ hãi chút nào. Bác sĩ rất nhẹ nhàng với thú cưng.',                  '2024-05-12 16:00:00', 'Cảm ơn chị Bình! Hẹn gặp lại Miu Miu vào lần tiêm kế tiếp nhé.',                    '2024-05-12 17:00:00'),
(3, 2, 3, 6,  4, 'Max được tắm sấy sạch sẽ, thơm tho cả tuần. Groomer rất khéo tay, chỉ hơi lâu một chút.',              '2024-05-15 12:00:00', 'Cảm ơn anh Cường đã phản hồi! Chúng tôi sẽ cải thiện thời gian để phục vụ tốt hơn.', '2024-05-15 16:00:00'),
(4, 2, 4, 9,  5, 'Bella được tạo kiểu lông cực đẹp, y hệt ảnh mẫu! Groomer chuyên nghiệp, Bella cũng rất thích.',        '2024-05-18 13:00:00', 'Cảm ơn chị Dung! Bella xinh lắm, hẹn gặp lại lần sau nhé!',                          '2024-05-18 15:00:00'),
(5, 3, 1, 11, 5, 'Rocky ở đây rất thoải mái! Nhân viên chăm sóc tận tình, camera cho tôi xem Rocky 24/7 rất yên tâm.',   '2024-05-29 10:00:00', 'Cảm ơn anh An! Rocky rất ngoan, là khách thú cưng được yêu thích nhất tuần.',         '2024-05-29 14:00:00'),
(6, 3, 5, 12, 5, 'Simba ở phòng VIP rất sung sướng, chăm sóc như ở nhà. Nhất định sẽ gửi lại lần sau!',                  '2024-06-04 10:00:00', NULL, NULL);

-- 15. NOTIFICATIONS
INSERT INTO notification (id, user_id, title, content, broadcast_id, notification_type, is_read, created_at) VALUES
(1,  1, 'Đặt lịch thành công',   'Bạn đã đặt lịch khám sức khỏe cho Lucky vào 10/05/2024 lúc 10:00',           NULL,           'BOOKING',   1, '2024-05-08 15:30:00'),
(2,  1, 'Thanh toán thành công', 'Thanh toán 180.000đ cho dịch vụ khám sức khỏe Lucky đã thành công',           NULL,           'BOOKING',   1, '2024-05-08 15:31:00'),
(3,  1, 'Dịch vụ hoàn tất',      'Lucky đã được khám xong tại Dr. Minh Pet Clinic. Xem kết quả khám!',          NULL,           'BOOKING',   1, '2024-05-10 10:35:00'),
(4,  2, 'Đặt lịch thành công',   'Bạn đã đặt lịch tiêm phòng cho Miu Miu vào 12/05/2024 lúc 14:00',            NULL,           'BOOKING',   1, '2024-05-10 09:15:00'),
(5,  3, 'Đặt lịch thành công',   'Bạn đã đặt lịch tắm sấy và cắt tỉa lông cho Max vào 15/05/2024 lúc 09:00',  NULL,           'BOOKING',   1, '2024-05-13 11:20:00'),
(6,  4, 'Đặt lịch thành công',   'Bạn đã đặt lịch tạo kiểu lông cho Bella vào 18/05/2024 lúc 10:00',           NULL,           'BOOKING',   1, '2024-05-16 14:10:00'),
(7,  1, 'Check-in thành công',   'Rocky đã check-in vào Pet Paradise Hotel. Xem camera theo dõi thú cưng!',     NULL,           'BOOKING',   1, '2024-05-25 08:05:00'),
(8,  1, 'Cập nhật chăm sóc',     'Rocky vừa được ăn sáng và đang vui vẻ chơi trong sân',                        NULL,           'SYSTEM',    1, '2024-05-25 08:35:00'),
(9,  1, 'Check-out thành công',  'Rocky đã check-out. Cảm ơn bạn đã tin tưởng Pet Paradise Hotel!',             NULL,           'BOOKING',   1, '2024-05-28 18:05:00'),
(10, 5, 'Check-in thành công',   'Simba đã check-in vào Pet Paradise Hotel phòng VIP',                          NULL,           'BOOKING',   1, '2024-05-30 10:05:00'),
(11, 5, 'Check-out thành công',  'Simba đã check-out. Hẹn gặp lại tại Pet Paradise Hotel!',                     NULL,           'BOOKING',   1, '2024-06-03 10:05:00'),
(12, 1, 'Nhắc nhở lịch hẹn',    'Lucky có lịch tắm sấy tại Happy Paws vào 28/06/2026 lúc 10:00',               NULL,           'REMINDER',  0, '2026-06-27 08:00:00'),
(13, 2, 'Nhắc nhở lịch hẹn',    'Miu Miu có lịch spa vào 22/06/2026 lúc 11:00 tại Happy Paws',                 NULL,           'REMINDER',  0, '2026-06-21 08:00:00'),
(14, 1, 'Khuyến mãi thú cưng',  'Giảm 20% tất cả dịch vụ spa & grooming trong tháng 6 tại Happy Paws!',       'PROMO-2026-06','PROMOTION', 0, '2026-06-01 00:00:00'),
(15, 2, 'Khuyến mãi thú cưng',  'Giảm 20% tất cả dịch vụ spa & grooming trong tháng 6 tại Happy Paws!',       'PROMO-2026-06','PROMOTION', 0, '2026-06-01 00:00:00'),
(16, 3, 'Khuyến mãi thú cưng',  'Giảm 20% tất cả dịch vụ spa & grooming trong tháng 6 tại Happy Paws!',       'PROMO-2026-06','PROMOTION', 0, '2026-06-01 00:00:00'),
(17, 1, 'Đặt lịch thành công',  'Bạn đã đặt lịch tiêm phòng cho Rocky vào 15/04/2024 lúc 09:00',               NULL,           'BOOKING',   1, '2024-04-13 08:20:00'),
(18, 1, 'Dịch vụ hoàn tất',     'Rocky đã tiêm phòng xong tại Dr. Minh Pet Clinic',                             NULL,           'BOOKING',   1, '2024-04-15 09:35:00');

SET FOREIGN_KEY_CHECKS = 1;
