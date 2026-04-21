CREATE TABLE categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id VARCHAR(64) NOT NULL,
  name VARCHAR(120) NOT NULL,
  parent_id BIGINT NULL,
  level INT NOT NULL,
  CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id),
  CONSTRAINT chk_categories_level CHECK (level BETWEEN 1 AND 4),
  UNIQUE KEY uq_category_name_per_parent (merchant_id, parent_id, name),
  INDEX idx_categories_merchant (merchant_id),
  INDEX idx_categories_parent (parent_id)
);

CREATE TABLE brands (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id VARCHAR(64) NOT NULL,
  name VARCHAR(120) NOT NULL,
  UNIQUE KEY uq_brand_name_per_merchant (merchant_id, name),
  INDEX idx_brands_merchant (merchant_id)
);

CREATE TABLE products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id VARCHAR(64) NOT NULL,
  product_code VARCHAR(64) NOT NULL,
  name VARCHAR(200) NOT NULL,
  brand_id BIGINT NULL,
  category_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_products_product_code (product_code),
  INDEX idx_products_merchant (merchant_id),
  CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands(id),
  CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE skus (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id VARCHAR(64) NOT NULL,
  product_id BIGINT NOT NULL,
  barcode VARCHAR(64) NOT NULL,
  stock_quantity INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_skus_barcode (barcode),
  INDEX idx_skus_merchant (merchant_id),
  INDEX idx_skus_product (product_id),
  CONSTRAINT fk_skus_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  merchant_id VARCHAR(64) NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  read_at TIMESTAMP NULL,
  INDEX idx_notifications_user (user_id),
  INDEX idx_notifications_merchant (merchant_id)
);

