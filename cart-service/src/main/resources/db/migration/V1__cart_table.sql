CREATE TABLE t_cart (
    customer_id varchar(255) NOT NULL,
    product_id varchar(255) NOT NULL,
    product_name varchar(255) NOT NULL,
    amount int NOT NULL,
    PRIMARY KEY (customer_id, product_id)
);