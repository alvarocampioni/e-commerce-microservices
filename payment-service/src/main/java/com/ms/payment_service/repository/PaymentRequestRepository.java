package com.ms.payment_service.repository;

import com.ms.payment_service.model.PaymentRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRequestRepository extends MongoRepository<PaymentRequest, String> {
    PaymentRequest findBySessionId(String sessionId);
}
