CREATE TABLE t_order (
    id varchar(255) NOT NULL,
    customer_id varchar(255) NOT NULL,
    product_id varchar(255) NOT NULL,
    product_name varchar(255) NOT NULL,
    amount int NOT NULL,
    price decimal(10, 2) NOT NULL,
    is_Archived bool NOT NULL ,
    status int NOT NULL,
    order_date DATETIME NOT NULL,
    execution_date DATETIME,
    PRIMARY KEY (id, customer_id, product_id)
);