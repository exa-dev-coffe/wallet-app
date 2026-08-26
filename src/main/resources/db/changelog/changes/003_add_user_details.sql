-- liquibase formatted sql

-- changeset developer:3
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'td_balance_histories' AND column_name = 'user_email';
ALTER TABLE td_balance_histories
    ADD COLUMN user_email VARCHAR(255),
    ADD COLUMN user_name VARCHAR(255);

-- rollback ALTER TABLE td_balance_histories DROP COLUMN user_email, DROP COLUMN user_name;
