CREATE TABLE IF NOT EXISTS `wa_message_receipts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `message_id` int(11) NOT NULL,
  `recipient_id` int(11) NOT NULL,
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0=pending, 1=delivered, 2=seen',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_recipient_status` (`recipient_id`, `status`),
  KEY `idx_message_id` (`message_id`),
  KEY `idx_recipient_message` (`recipient_id`, `message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
