package com.ms.email_service.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record OrderProduct(String id, String customerId, String productId, String productName, BigDecimal price, int amount) implements Serializable{
}