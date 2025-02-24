package com.ms.order_service.dto;

import java.io.Serializable;
import java.util.List;

public record OrderCanceledDTO(String email, List<String> unavailable) implements Serializable {
}
