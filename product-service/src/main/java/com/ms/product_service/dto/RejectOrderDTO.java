package com.ms.product_service.dto;

import java.util.List;

public record RejectOrderDTO (String orderId, String email, List<String> unavailable) {
}
