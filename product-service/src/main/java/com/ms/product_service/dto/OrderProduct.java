package com.ms.product_service.dto;

import java.math.BigDecimal;

public record OrderProduct(String id, String email, String productId, String productName, BigDecimal price, int amount) {
}
