-- Second MERCHANT user on mrc_A (same password as merchantA: merchantA123!)
DELETE FROM users WHERE username = 'merchantA2';
INSERT INTO users (username, password_hash, role, merchant_id, failed_attempts, locked_until)
VALUES (
  'merchantA2',
  'pbkdf2_sha256$310000$32rgXe5m066wkG5MPxQ0dA==$ttPVxlDtccLXJfqh2Tr06Ot2GBTuuUCf1W5wBH4dd4U=',
  'MERCHANT',
  'mrc_A',
  0,
  NULL
);
