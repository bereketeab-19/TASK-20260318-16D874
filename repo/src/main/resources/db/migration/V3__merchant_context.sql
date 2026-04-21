ALTER TABLE users
  ADD COLUMN merchant_id VARCHAR(64) NULL;

-- MySQL 8+ supports CHECK constraints, but enforcement can vary by configuration.
-- This constraint is still valuable for static auditability; service-layer validation also applies.
ALTER TABLE users
  ADD CONSTRAINT chk_users_merchant_id_role
  CHECK (
    (role = 'MERCHANT' AND merchant_id IS NOT NULL)
    OR
    (role <> 'MERCHANT' AND merchant_id IS NULL)
  );

CREATE INDEX idx_users_merchant_id ON users (merchant_id);

-- Seed merchant users for tenant isolation verification
-- Passwords:
-- merchantA / merchantA123!
-- merchantB / merchantB123!
INSERT INTO users (username, password_hash, role, merchant_id, failed_attempts, locked_until)
VALUES
  ('merchantA', 'pbkdf2_sha256$310000$32rgXe5m066wkG5MPxQ0dA==$ttPVxlDtccLXJfqh2Tr06Ot2GBTuuUCf1W5wBH4dd4U=', 'MERCHANT', 'mrc_A', 0, NULL),
  ('merchantB', 'pbkdf2_sha256$310000$EkY1NsZSAoWmRt6XdXpbZA==$nMtR8+ZMv9rPZWVxr5e4Rg+Y73F1omvd+5IAuRoHmQc=', 'MERCHANT', 'mrc_B', 0, NULL);

