package com.ms.email_service.dto;

import java.io.Serializable;
import java.util.List;

public record RejectOrderDTO(String orderId, String email, List<String> unavailable) implements Serializable {
}
