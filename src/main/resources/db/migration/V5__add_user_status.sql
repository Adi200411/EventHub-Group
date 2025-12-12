-- V4__add_user_status_column.sql
-- Adds account status tracking for admin actions

ALTER TABLE Users
ADD COLUMN status ENUM('ACTIVE', 'DEACTIVATED', 'BANNED') DEFAULT 'ACTIVE';

-- Update existing users to ACTIVE by default
UPDATE Users SET status = 'ACTIVE' WHERE status IS NULL;
