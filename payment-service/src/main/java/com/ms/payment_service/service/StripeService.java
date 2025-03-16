package com.ms.payment_service.service;

import com.ms.payment_service.dto.OrderDTO;
import com.ms.payment_service.dto.OrderProduct;
import com.ms.payment_service.dto.PaymentCreatedDTO;
import com.ms.payment_service.dto.PaymentStatusChangedDTO;
import com.ms.payment_service.events.StripeEventProducer;
import com.ms.payment_service.model.PaymentRequest;
import com.ms.payment_service.model.Status;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class StripeService {

    private final StripeEventProducer eventProducer;
    private final PaymentRequestService paymentRequestService;

    @Value("${STRIPE.API.KEY}")
    private String stripeSecretKey;

    @Autowired
    public StripeService(StripeEventProducer eventProducer, PaymentRequestService paymentRequestService) {
        this.eventProducer = eventProducer;
        this.paymentRequestService = paymentRequestService;
    }

    public void processOrderCreation(OrderDTO order) throws StripeException {
        Session session = generateCheckoutSession(order);
        if(session != null) {
            // notify user
            String userEmail = order.order().getFirst().email();
            eventProducer.paymentCreated(new PaymentCreatedDTO(userEmail, order, session.getUrl()));
            // store session data
            paymentRequestService.createPaymentRequest(new PaymentRequest(order.order().getFirst().email(), order.order().getFirst().id(), session.getId(), Status.CREATED));
        } else {
            throw new RuntimeException("Session is null");
        }
    }

    private Session generateCheckoutSession(OrderDTO order) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        if (order != null && !order.order().isEmpty()) {
            for (OrderProduct product : order.order()) {
                String stringPrice = String.valueOf(product.price());
                double parsedPrice = Double.parseDouble(stringPrice);
                long longPrice = (long) (parsedPrice * 100L);
                lineItems.add(SessionCreateParams.LineItem.builder()
                        .setQuantity((long) product.amount())
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("USD")
                                .setUnitAmount(longPrice)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(product.productName())
                                        .build())
                                .build())
                        .build());
            }
            long currTime = Instant.now().getEpochSecond();
            // 30min until expire
            long offset = 1800L;
            SessionCreateParams params = SessionCreateParams.builder()
                    .addAllLineItem(lineItems)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://payment-service:8084/api/payment/success")
                    .setCancelUrl("http://payment-service:8084/api/payment/fail")
                    .putMetadata("orderId", order.order().getFirst().id())
                    .setPaymentIntentData(
                            SessionCreateParams.PaymentIntentData.builder()
                                    .putMetadata("orderId", order.order().getFirst().id())
                                    .build()
                    )
                    .setExpiresAt(currTime + offset)
                    .build();
            return Session.create(params);
        }
        return null;
    }

    public void cancelPayment(String orderId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        PaymentRequest paymentRequest = paymentRequestService.getPaymentRequestById(orderId);
        Session session = Session.retrieve(paymentRequest.getSessionId());
        session.expire();

        paymentRequestService.setStatus(orderId, Status.CANCELED);
        eventProducer.paymentCanceled(new PaymentStatusChangedDTO(paymentRequest.getEmail(), orderId));
    }

}
