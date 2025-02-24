package com.ms.payment_service.service;

import com.ms.payment_service.model.PaymentRequest;
import com.ms.payment_service.model.Status;
import com.ms.payment_service.repository.PaymentRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentRequestService {

    private final PaymentRequestRepository paymentRequestRepository;

    @Autowired
    public PaymentRequestService(PaymentRequestRepository paymentRequestRepository) {
        this.paymentRequestRepository = paymentRequestRepository;
    }

    public PaymentRequest getPaymentRequestById(String orderId) {
        Optional<PaymentRequest> paymentRequest = paymentRequestRepository.findById(orderId);
        return paymentRequest.orElse(null);
    }

    public void createPaymentRequest(PaymentRequest paymentRequest){
        paymentRequestRepository.save(paymentRequest);
    }

    public void setStatus(String orderId, Status status){
        Optional<PaymentRequest> optional = paymentRequestRepository.findById(orderId);
        if(optional.isPresent()){
            PaymentRequest paymentRequest = optional.get();
            paymentRequest.setStatus(status);
            paymentRequestRepository.save(paymentRequest);
        }
    }

    public String getEmail(String orderId){
        Optional<PaymentRequest> optional = paymentRequestRepository.findById(orderId);
        return optional.map(PaymentRequest::getEmail).orElse(null);
    }
}
