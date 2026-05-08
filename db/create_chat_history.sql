-- ============================================================
-- Tạo bảng chat_history cho PetEye Chatbot
-- Chạy file này 1 lần trong MySQL Workbench hoặc CLI:
--   mysql -u root -p PET_EYE < create_chat_history.sql
-- ============================================================

USE PET_EYE;

CREATE TABLE IF NOT EXISTS chat_history (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL,
    role        VARCHAR(20)  NOT NULL COMMENT '"user" hoặc "assistant"',
    content     TEXT         NOT NULL,
    tool_result_json TEXT    NULL     COMMENT 'JSON string của tool result để render lại card',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_chat_history_user_id (user_id),
    INDEX idx_chat_history_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
