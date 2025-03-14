package com.ms.order_service.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record CartProductDTO(String customerId, String productId, String productName, int amount) implements Serializable {
}
