-- Creates DB users for runtime and migrations.
-- Executed by MySQL docker-entrypoint as root.

CREATE USER IF NOT EXISTS 'migrator_user'@'%' IDENTIFIED BY 'migrator_pass';
CREATE USER IF NOT EXISTS 'app_user'@'%' IDENTIFIED BY 'app_pass';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES, DROP, TRIGGER ON petsupplies.* TO 'migrator_user'@'%' WITH GRANT OPTION;

GRANT SELECT, INSERT, UPDATE, DELETE ON petsupplies.* TO 'app_user'@'%';

FLUSH PRIVILEGES;

