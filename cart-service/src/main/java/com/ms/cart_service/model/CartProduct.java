package com.ms.cart_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "t_cart")
@Data
@AllArgsConstructor
@NoArgsConstructor
@IdClass(CartProductId.class)
public class CartProduct implements Serializable {

    @Id
    @Column(name = "customer_id")
    private String customerId;
    @Id
    @Column(name = "product_id")
    private String productId;
    @Column(name = "product_name")
    private String productName;
    @Column(name = "amount")
    private int amount;

}
