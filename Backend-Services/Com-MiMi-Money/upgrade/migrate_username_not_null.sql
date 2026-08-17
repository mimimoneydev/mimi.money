-- Migration: Make username NOT NULL
-- Date: 2026-05-04
-- Description: Update wa_users table to require username for all new users

-- Step 1: Set default username for existing NULL usernames
UPDATE wa_users SET username = CONCAT('User', id) WHERE username IS NULL OR username = '';

-- Step 2: Alter table to make username NOT NULL with default
ALTER TABLE wa_users MODIFY COLUMN username VARCHAR(255) NOT NULL DEFAULT 'User';

-- Step 3: Remove the default (optional - forces new users to provide name)
ALTER TABLE wa_users ALTER COLUMN username DROP DEFAULT;
