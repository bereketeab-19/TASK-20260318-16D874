CREATE TABLE cooking_processes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  current_step_index INT NOT NULL DEFAULT 0,
  last_checkpoint_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_cooking_processes_merchant_user (merchant_id, user_id)
);

CREATE TABLE cooking_steps (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  process_id BIGINT NOT NULL,
  step_index INT NOT NULL,
  instruction TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_cooking_steps_process_step (process_id, step_index),
  CONSTRAINT fk_cooking_steps_process FOREIGN KEY (process_id) REFERENCES cooking_processes(id)
);

CREATE TABLE cooking_timers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  process_id BIGINT NOT NULL,
  label VARCHAR(120) NOT NULL,
  start_timestamp TIMESTAMP NOT NULL,
  duration_seconds INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_cooking_timers_process (process_id),
  CONSTRAINT fk_cooking_timers_process FOREIGN KEY (process_id) REFERENCES cooking_processes(id)
);

CREATE TABLE achievements (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  period VARCHAR(120) NOT NULL,
  responsible_person VARCHAR(120) NOT NULL,
  conclusion TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_achievements_merchant_user (merchant_id, user_id)
);

CREATE TABLE achievement_attachment_versions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  achievement_id BIGINT NOT NULL,
  version INT NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  content_type VARCHAR(80) NOT NULL,
  size_bytes BIGINT NOT NULL,
  storage_path VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_ach_attachment_version (achievement_id, version),
  INDEX idx_ach_attachment_achievement (achievement_id),
  CONSTRAINT fk_ach_attachment_achievement FOREIGN KEY (achievement_id) REFERENCES achievements(id)
);

