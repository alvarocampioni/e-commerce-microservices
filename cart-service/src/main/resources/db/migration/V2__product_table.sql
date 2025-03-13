CREATE TABLE t_product (
    product_id varchar(255) NOT NULL,
    product_name varchar(255) NOT NULL,
    product_price decimal(10, 2) NOT NULL ,
    amount int NOT NULL,
    PRIMARY KEY (product_id)
);