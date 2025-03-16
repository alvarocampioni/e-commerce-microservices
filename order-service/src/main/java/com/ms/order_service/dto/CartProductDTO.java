package com.ms.order_service.dto;

import java.io.Serializable;

public record CartProductDTO(String email, String productId, String productName, int amount) implements Serializable {
}
