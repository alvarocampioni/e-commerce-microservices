package com.ms.order_service.dto;

import java.io.Serializable;

public record PaymentStatusChangedDTO (String email, String orderId) implements Serializable {
}
