-- liquibase formatted sql

-- changeset developer:4 validCheckSum:ANY
-- Add wallet_number to tm_balances table

ALTER TABLE tm_balances ADD COLUMN IF NOT EXISTS wallet_number VARCHAR(30);

-- Populate existing rows with default wallet_number format (8839 + 8-digit user_id)
UPDATE tm_balances 
SET wallet_number = '8839' || LPAD(user_id::text, 8, '0')
WHERE wallet_number IS NULL OR wallet_number = '';

-- rollback ALTER TABLE tm_balances DROP COLUMN IF EXISTS wallet_number;

