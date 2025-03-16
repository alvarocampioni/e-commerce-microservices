CREATE TABLE t_order (
    id varchar(255) NOT NULL,
    email varchar(255) NOT NULL,
    product_id varchar(255) NOT NULL,
    product_name varchar(255) NOT NULL,
    amount int NOT NULL,
    price decimal(10, 2),
    is_Archived bool NOT NULL ,
    status int NOT NULL,
    order_date DATETIME(3) NOT NULL,
    execution_date DATETIME(3),
    PRIMARY KEY (id, email, product_id)
);