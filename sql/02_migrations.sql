-- -------------------------------------------------------
-- Incremental migrations — safe to run on existing data
-- Each ALTER uses IF NOT EXISTS / IF EXISTS guards.
-- -------------------------------------------------------

-- M001: add eligible_after column to payment_reminders
ALTER TABLE payment_reminders
    ADD COLUMN IF NOT EXISTS eligible_after DATETIME NULL
        COMMENT 'NULL = immediately eligible; set to sent_at+15d for SECOND reminders';
