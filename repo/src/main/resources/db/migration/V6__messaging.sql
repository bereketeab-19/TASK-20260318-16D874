CREATE TABLE sessions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_sessions_merchant (merchant_id)
);

CREATE TABLE attachments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id VARCHAR(64) NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  content_type VARCHAR(80) NOT NULL,
  size_bytes BIGINT NOT NULL,
  storage_path VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_attachments_sha256 (sha256),
  INDEX idx_attachments_merchant (merchant_id)
);

CREATE TABLE messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id VARCHAR(64) NOT NULL,
  session_id BIGINT NOT NULL,
  sender_username VARCHAR(80) NOT NULL,
  content TEXT NULL,
  content_hash VARCHAR(64) NULL,
  attachment_id BIGINT NULL,
  sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_messages_merchant_session (merchant_id, session_id),
  INDEX idx_messages_sent_at (sent_at),
  CONSTRAINT fk_messages_session FOREIGN KEY (session_id) REFERENCES sessions(id),
  CONSTRAINT fk_messages_attachment FOREIGN KEY (attachment_id) REFERENCES attachments(id)
);

