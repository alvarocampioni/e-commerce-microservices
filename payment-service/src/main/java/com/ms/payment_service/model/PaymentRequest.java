package com.ms.payment_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "payment")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private String email;
    @Id
    private String orderId;
    private String sessionId;
    private Status status;
}
