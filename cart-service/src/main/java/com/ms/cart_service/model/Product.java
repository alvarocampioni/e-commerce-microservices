package com.ms.cart_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "t_product")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    @Id
    private String productId;
    private String productName;
    private BigDecimal productPrice;
    private int amount;
}
