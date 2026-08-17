CREATE TABLE IF NOT EXISTS `wa_pending_wallet_messages` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `message_id` int(11) NOT NULL,
  `sender_id` int(11) NOT NULL,
  `recipient_wallet_address` varchar(255) NOT NULL,
  `resolved_recipient_id` int(11) DEFAULT NULL,
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0=pending, 1=resolved',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_message_wallet` (`message_id`, `recipient_wallet_address`),
  KEY `idx_wallet_status` (`recipient_wallet_address`, `status`),
  KEY `idx_resolved_recipient` (`resolved_recipient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
