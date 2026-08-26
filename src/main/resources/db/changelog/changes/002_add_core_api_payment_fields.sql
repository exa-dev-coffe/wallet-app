-- liquibase formatted sql

-- changeset developer:2
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'td_balance_histories' AND column_name = 'payment_type';
ALTER TABLE td_balance_histories
    ADD COLUMN payment_type VARCHAR(50),
    ADD COLUMN bank VARCHAR(50),
    ADD COLUMN va_number VARCHAR(100),
    ADD COLUMN bill_key VARCHAR(100),
    ADD COLUMN biller_code VARCHAR(100),
    ADD COLUMN qr_url TEXT,
    ADD COLUMN qr_string TEXT,
    ADD COLUMN deeplink_url TEXT,
    ADD COLUMN expiry_time VARCHAR(100);

-- rollback ALTER TABLE td_balance_histories DROP COLUMN payment_type, DROP COLUMN bank, DROP COLUMN va_number, DROP COLUMN bill_key, DROP COLUMN biller_code, DROP COLUMN qr_url, DROP COLUMN qr_string, DROP COLUMN deeplink_url, DROP COLUMN expiry_time;
