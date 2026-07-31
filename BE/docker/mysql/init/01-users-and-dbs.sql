CREATE DATABASE IF NOT EXISTS pokemon_tcg_dev;
CREATE DATABASE IF NOT EXISTS pokemon_tcg_prod;

CREATE USER IF NOT EXISTS 'pokemon_dev'@'%' IDENTIFIED BY 'dev_password';
ALTER USER 'pokemon_dev'@'%' IDENTIFIED BY 'dev_password';

CREATE USER IF NOT EXISTS 'pokemon_prod'@'%' IDENTIFIED BY 'prod_password';
ALTER USER 'pokemon_prod'@'%' IDENTIFIED BY 'prod_password';

GRANT ALL PRIVILEGES ON pokemon_tcg_dev.* TO 'pokemon_dev'@'%';
GRANT ALL PRIVILEGES ON pokemon_tcg_prod.* TO 'pokemon_prod'@'%';
FLUSH PRIVILEGES;