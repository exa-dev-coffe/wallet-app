ALTER TABLE td_balance_histories
    ADD COLUMN IF NOT EXISTS user_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS user_name VARCHAR(255);
