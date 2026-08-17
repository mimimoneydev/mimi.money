ALTER TABLE `wa_messages` ADD COLUMN `status` INT(11) NOT NULL DEFAULT 1 COMMENT '0=Waiting, 1=Sent, 2=Delivered, 3=Seen';
ALTER TABLE `wa_messages` ADD INDEX `idx_status` (`status`);
