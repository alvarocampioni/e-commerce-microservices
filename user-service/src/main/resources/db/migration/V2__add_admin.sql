INSERT INTO t_user (email, password, role, code, is_verified, created) values (
    'youremail@gmail.com',
    '$2a$04$OEKkWbF9X5e97Jkp8qOPs.ZZr8M11pBwqJjWXb1nBDMwUtWT9DuIO', #password: 12345
    'ADMIN',
    'AAAAA',
    true,
    CURRENT_TIMESTAMP
);