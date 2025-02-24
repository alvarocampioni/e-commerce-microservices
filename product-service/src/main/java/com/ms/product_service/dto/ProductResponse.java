package com.ms.product_service.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record ProductResponse(String productId, String productName, BigDecimal productPrice, int amount, ProductCategory productCategory) implements Serializable {
}
