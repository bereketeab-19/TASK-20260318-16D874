DROP TRIGGER IF EXISTS categories_before_insert;
DROP TRIGGER IF EXISTS categories_before_update;

DELIMITER $$

CREATE TRIGGER categories_before_insert
BEFORE INSERT ON categories
FOR EACH ROW
BEGIN
  DECLARE parentLevel INT;
  DECLARE parentMerchant VARCHAR(64);

  IF NEW.parent_id IS NULL THEN
    SET NEW.level = 1;
  ELSE
    SELECT level, merchant_id INTO parentLevel, parentMerchant FROM categories WHERE id = NEW.parent_id;
    IF parentMerchant IS NULL THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid parent category';
    END IF;
    IF parentMerchant <> NEW.merchant_id THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Parent category merchant mismatch';
    END IF;
    SET NEW.level = parentLevel + 1;
  END IF;

  IF NEW.level > 4 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Category depth exceeds 4';
  END IF;
END$$

CREATE TRIGGER categories_before_update
BEFORE UPDATE ON categories
FOR EACH ROW
BEGIN
  DECLARE parentLevel INT;
  DECLARE parentMerchant VARCHAR(64);

  IF NEW.parent_id IS NULL THEN
    SET NEW.level = 1;
  ELSE
    SELECT level, merchant_id INTO parentLevel, parentMerchant FROM categories WHERE id = NEW.parent_id;
    IF parentMerchant IS NULL THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid parent category';
    END IF;
    IF parentMerchant <> NEW.merchant_id THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Parent category merchant mismatch';
    END IF;
    SET NEW.level = parentLevel + 1;
  END IF;

  IF NEW.level > 4 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Category depth exceeds 4';
  END IF;
END$$

DELIMITER ;

