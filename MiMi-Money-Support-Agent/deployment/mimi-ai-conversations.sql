CREATE TABLE IF NOT EXISTS chatbull_mimi_ai_conversations (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    public_token CHAR(64) NOT NULL,
    chat_session_id INT UNSIGNED NOT NULL,
    visitor_id INT UNSIGNED NOT NULL,
    state ENUM('ai', 'handoff', 'closed') NOT NULL DEFAULT 'ai',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_public_token (public_token),
    KEY idx_chat_session (chat_session_id),
    CONSTRAINT fk_mimi_ai_session FOREIGN KEY (chat_session_id)
        REFERENCES chatbull_chat_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_mimi_ai_visitor FOREIGN KEY (visitor_id)
        REFERENCES chatbull_users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
