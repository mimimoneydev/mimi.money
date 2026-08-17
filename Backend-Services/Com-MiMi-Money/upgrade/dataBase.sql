-- phpMyAdmin SQL Dump
-- version 4.4.10
-- http://www.phpmyadmin.net
--
-- Host: localhost:8889
-- Generation Time: Jun 25, 2017 at 05:56 AM
-- Server version: 5.5.42
-- PHP Version: 7.0.0

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

--
-- Database: `whatsCloneF`
--


  ALTER TABLE `wa_groups`
    CHANGE `name` `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

  ALTER TABLE `wa_status`
        CHANGE `status` `status` varchar(225) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

  CREATE TABLE IF NOT EXISTS `wa_user_labels` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `owner_user_id` int(10) NOT NULL,
    `target_user_id` int(10) NOT NULL,
    `label` varchar(255) NOT NULL,
    `updated_at` int(11) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `owner_target_unique` (`owner_user_id`,`target_user_id`),
    KEY `owner_user_id` (`owner_user_id`),
    KEY `target_user_id` (`target_user_id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
