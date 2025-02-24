CREATE DATABASE IF NOT EXISTS user_service;
CREATE DATABASE IF NOT EXISTS order_service;
CREATE DATABASE IF NOT EXISTS cart_service;

CREATE USER 'user'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON user_service_db.* TO 'user'@'%';
GRANT ALL PRIVILEGES ON order_service_db.* TO 'user'@'%';
GRANT ALL PRIVILEGES ON cart_service_db.* TO 'user'@'%';
FLUSH PRIVILEGES;

FLUSH PRIVILEGES;
