package com.ms.product_service.dto;

import java.util.List;

public record RejectOrderDTO (String orderId, String customerId, List<String> unavailable) {
}
