-- DB-level hardening: audit_logs is immutable append-only.
--
-- MySQL privilege REVOKE on a specific table can fail when the user only has
-- database-level privileges. To keep startup reliable and the rule statically
-- verifiable, we enforce immutability with triggers that reject UPDATE/DELETE.

DROP TRIGGER IF EXISTS audit_logs_block_update;
DROP TRIGGER IF EXISTS audit_logs_block_delete;

DELIMITER $$

CREATE TRIGGER audit_logs_block_update
BEFORE UPDATE ON audit_logs
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_logs is append-only';
END$$

CREATE TRIGGER audit_logs_block_delete
BEFORE DELETE ON audit_logs
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_logs is append-only';
END$$

DELIMITER ;

