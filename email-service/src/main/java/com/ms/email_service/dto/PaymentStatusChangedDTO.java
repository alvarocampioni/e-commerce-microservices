package com.ms.email_service.dto;

import java.io.Serializable;

public record PaymentStatusChangedDTO (String email, String orderId) implements Serializable{
}
