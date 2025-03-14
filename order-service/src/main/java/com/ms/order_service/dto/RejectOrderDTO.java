package com.ms.order_service.dto;

import java.io.Serializable;

public record RejectOrderDTO(String orderId) implements Serializable {
}
