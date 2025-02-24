CREATE TABLE t_user (
    email varchar(255) NOT NULL PRIMARY KEY ,
    password varchar(255) NOT NULL,
    role enum('CUSTOMER', 'ADMIN') NOT NULL,
    code varchar(5) NOT NULL,
    is_verified bool NOT NULL,
    created timestamp NOT NULL
);