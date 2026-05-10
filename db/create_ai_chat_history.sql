-- ============================================================
-- Tạo bảng lịch sử chat AI cho Shop Owner và Admin
-- Chạy file này 1 lần trong MySQL Workbench:
--   USE PET_EYE; source create_ai_chat_history.sql;
-- ============================================================

USE PET_EYE;

-- Shop AI chat history
CREATE TABLE IF NOT EXISTS shop_ai_chat_history (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    shop_id     INT          NOT NULL,
    role        VARCHAR(20)  NOT NULL COMMENT '"user" hoặc "assistant"',
    content     TEXT         NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_shop_ai_shop_id (shop_id),
    INDEX idx_shop_ai_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Admin AI chat history
CREATE TABLE IF NOT EXISTS admin_ai_chat_history (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL,
    role        VARCHAR(20)  NOT NULL COMMENT '"user" hoặc "assistant"',
    content     TEXT         NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_admin_ai_user_id (user_id),
    INDEX idx_admin_ai_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
