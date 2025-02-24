package com.ms.product_service.model;

import com.ms.product_service.dto.ProductCategory;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.math.BigDecimal;

@Document(collection = "product")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Serializable {

    @Id
    private String id;
    @Version
    private int version;
    private String name;
    private String description;
    private BigDecimal price;
    private ProductCategory category;
    private int amount;

    public Product(String name, String description, BigDecimal price, ProductCategory category, int amount) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.amount = amount;
    }
}
