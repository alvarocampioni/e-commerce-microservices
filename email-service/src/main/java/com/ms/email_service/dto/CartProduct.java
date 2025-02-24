package com.ms.email_service.dto;

import java.io.Serializable;

public record CartProduct(String customerId, String productId, String productName, int amount) implements Serializable {
}
