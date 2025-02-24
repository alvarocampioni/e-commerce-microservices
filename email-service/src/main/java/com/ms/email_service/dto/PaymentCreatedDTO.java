package com.ms.email_service.dto;

import java.io.Serializable;

public record PaymentCreatedDTO(String email, OrderDTO order, String url) implements Serializable {
}
