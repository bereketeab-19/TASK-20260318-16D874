-- Scope attachment deduplication per tenant (merchant_id + sha256), not globally by hash alone.
ALTER TABLE attachments DROP INDEX uq_attachments_sha256;
ALTER TABLE attachments ADD UNIQUE KEY uq_attachments_merchant_sha256 (merchant_id, sha256);
