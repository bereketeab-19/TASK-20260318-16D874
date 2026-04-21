CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(80) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL,
  failed_attempts INT NOT NULL DEFAULT 0,
  locked_until TIMESTAMP NULL
);

CREATE TABLE audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  action VARCHAR(120) NOT NULL,
  payload TEXT NOT NULL,
  timestamp TIMESTAMP NOT NULL,
  actor_username VARCHAR(80) NULL,
  ip VARCHAR(64) NULL,
  INDEX idx_audit_logs_timestamp (timestamp),
  INDEX idx_audit_logs_action (action)
);

-- Default ADMIN user
-- Password: admin123!  (documented in README.md)
-- NOTE: This hash is PBKDF2-HMAC-SHA256 encoded (see Pbkdf2Sha256PasswordEncoder).
INSERT INTO users (username, password_hash, role, failed_attempts, locked_until)
VALUES ('admin', 'pbkdf2_sha256$310000$4qPAv6gysiFlmeH8hwQO5Q==$AhY8tRwuI/KSBy0LGi7/lJ49PFdD1DA2C3TpB9+57KA=', 'ADMIN', 0, NULL);

-- Default BUYER user (for role-boundary verification)
-- Password: buyer123!
INSERT INTO users (username, password_hash, role, failed_attempts, locked_until)
VALUES ('buyer', 'pbkdf2_sha256$310000$akutny0/ERAV2jc+bZbmIQ==$8tEQb4c2ZgtujMEZ0ABmWs3F5cuNiZ+nBszW0uWhcnA=', 'BUYER', 0, NULL);

