package com.ms.payment_service.controller;

import com.ms.payment_service.dto.PaymentStatusChangedDTO;
import com.ms.payment_service.events.StripeEventProducer;
import com.ms.payment_service.model.PaymentRequest;
import com.ms.payment_service.model.Status;
import com.ms.payment_service.model.StripeEventTypes;
import com.ms.payment_service.service.PaymentRequestService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/payment/webhook")
@Slf4j
public class StripeWebhookController {

    private final String whSecret;
    private final PaymentRequestService paymentRequestService;
    private final StripeEventProducer stripeEventProducer;

    @Autowired
    public StripeWebhookController(@Value("${STRIPE.WEBHOOK.SECRET}")String whSecret, PaymentRequestService paymentRequestService, StripeEventProducer stripeEventProducer) {
        this.whSecret = whSecret;
        this.paymentRequestService = paymentRequestService;
        this.stripeEventProducer = stripeEventProducer;
    }

    @PostMapping
    public void handleWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, whSecret);

            StripeEventTypes eventType = StripeEventTypes.fromString(event.getType());
            EventDataObjectDeserializer data = event.getDataObjectDeserializer();
            StripeObject stripeObject;

            if (data.getObject().isPresent()) {
                stripeObject = data.getObject().get();
            } else {
                throw new RuntimeException("Stripe event data object is empty");
            }

            switch (stripeObject) {
                case Session session when StripeEventTypes.CHECKOUT_SESSION_COMPLETED.equals(eventType) ->
                        updatePaymentData(session.getMetadata(), Status.SUCCEEDED);
                case Session session when StripeEventTypes.CHECKOUT_SESSION_EXPIRED.equals(eventType) ->
                        updatePaymentData(session.getMetadata(), Status.FAILED);
                case PaymentIntent paymentIntent when StripeEventTypes.PAYMENT_INTENT_SUCCEEDED.equals(eventType) ->
                        updatePaymentData(paymentIntent.getMetadata(), Status.SUCCEEDED);
                case PaymentIntent paymentIntent when StripeEventTypes.PAYMENT_INTENT_FAILED.equals(eventType) ->
                        updatePaymentData(paymentIntent.getMetadata(), Status.FAILED);
                default -> throw new IllegalStateException(event.getType());
            }
        } catch (Exception e) {
            log.error("Event not registered in the system: {}", e.getMessage());
        }
    }

    private void updatePaymentData(Map<String, String> data, Status status) {
        String orderId = data.get("orderId");
        PaymentRequest paymentRequest = paymentRequestService.getPaymentRequestById(orderId);
        if (paymentRequest != null && !paymentRequest.getStatus().equals(status) && !paymentRequest.getStatus().equals(Status.CANCELED)) {
            paymentRequestService.setStatus(orderId, status);
            String email = paymentRequestService.getEmail(orderId);
            if(status.equals(Status.FAILED)) {
                stripeEventProducer.paymentFailed(new PaymentStatusChangedDTO(email, orderId));
            } else if (status.equals(Status.SUCCEEDED)) {
                stripeEventProducer.paymentSucceeded(new PaymentStatusChangedDTO(email, orderId));
            }
        }
    }
}


