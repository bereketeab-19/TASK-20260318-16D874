CREATE TABLE technique_tags (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id VARCHAR(64) NOT NULL,
  name VARCHAR(120) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_technique_tags_merchant_name (merchant_id, name),
  INDEX idx_technique_tags_merchant (merchant_id)
);

CREATE TABLE technique_cards (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  body TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_technique_cards_merchant (merchant_id)
);

CREATE TABLE technique_card_tags (
  card_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  PRIMARY KEY (card_id, tag_id),
  CONSTRAINT fk_tct_card FOREIGN KEY (card_id) REFERENCES technique_cards(id) ON DELETE CASCADE,
  CONSTRAINT fk_tct_tag FOREIGN KEY (tag_id) REFERENCES technique_tags(id) ON DELETE CASCADE,
  INDEX idx_tct_tag (tag_id)
);
