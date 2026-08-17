CREATE TABLE IF NOT EXISTS chatbull_mimi_agenticous_jobs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT UNSIGNED NOT NULL,
    wallet_address VARCHAR(64) NOT NULL,
    agent_request_id VARCHAR(64) NULL,
    status ENUM('pending', 'settled', 'failed') NOT NULL DEFAULT 'pending',
    payment_network VARCHAR(64) NULL,
    payment_transaction VARCHAR(128) NULL,
    payment_payer VARCHAR(64) NULL,
    payment_recipient VARCHAR(64) NULL,
    payment_amount_usd DECIMAL(10,6) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_agenticous_conversation (conversation_id),
    KEY idx_agenticous_wallet (wallet_address),
    CONSTRAINT fk_agenticous_conversation FOREIGN KEY (conversation_id)
        REFERENCES chatbull_mimi_ai_conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
