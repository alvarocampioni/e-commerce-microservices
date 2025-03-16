package com.ms.email_service.service;

import com.ms.email_service.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    @Value("${spring.mail.username}")
    private String from;

    private final JavaMailSender javaMailSender;

    @Autowired
    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendUserNotification(UserNotificationDTO userNotificationDTO){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(userNotificationDTO.email());
        message.setSubject(userNotificationDTO.subject());
        message.setText(userNotificationDTO.content());
        javaMailSender.send(message);
    }

    public void paymentCreatedEmail(PaymentCreatedDTO paymentCreatedDTO) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setSubject("Order Placed");
        StringBuilder content = new StringBuilder();
        content.append("Your order has been placed on the following details:\n")
                .append("Order ID: ")
                .append(paymentCreatedDTO.order().order().getFirst().id())
                .append("\n\n")
                .append(buildOrderToString(paymentCreatedDTO.order()))
                .append("\n")
                .append("Perform the payment so we can start shipping the order !")
                .append("\n")
                .append("PAYMENT PAGE: ")
                .append(paymentCreatedDTO.url());
        message.setTo(paymentCreatedDTO.email());
        message.setText(content.toString());
        javaMailSender.send(message);
    }

    public void paymentSucceededEmail(PaymentStatusChangedDTO paymentStatusChangedDTO) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setSubject("Order Completed");
        StringBuilder content = new StringBuilder();
        content.append("Your payment has been successfully completed ! The products from your order will be shipped soon !")
                .append("\n\n")
                .append("Order ID: ")
                .append(paymentStatusChangedDTO.orderId())
                .append("\n")
                .append("Thank you for your preference !");
        message.setTo(paymentStatusChangedDTO.email());
        message.setText(content.toString());
        javaMailSender.send(message);
    }

    public void paymentFailedEmail(PaymentStatusChangedDTO paymentStatusChangedDTO) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setSubject("Order Canceled");
        StringBuilder content = new StringBuilder();
        content.append("Your payment didn't succeed ! Your order has been canceled !")
                .append("\n\n")
                .append("Order ID: ")
                .append(paymentStatusChangedDTO.orderId())
                .append("\n\n")
                .append("Sorry for any inconvenience !");
        message.setTo(paymentStatusChangedDTO.email());
        message.setText(content.toString());
        javaMailSender.send(message);
    }

    public void paymentCanceledEmail(PaymentStatusChangedDTO paymentStatusChangedDTO) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setSubject("Order Canceled");
        StringBuilder content = new StringBuilder();
        content.append("Your order has been canceled successfully !")
                .append("\n\n")
                .append("Order ID: ")
                .append(paymentStatusChangedDTO.orderId())
                .append("\n\n")
                .append("Sorry for any inconvenience !");
        message.setTo(paymentStatusChangedDTO.email());
        message.setText(content.toString());
        javaMailSender.send(message);
    }

    public void orderLoadedEmail(CartDTO cart){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        StringBuilder content = new StringBuilder();
        message.setSubject("Order Received");
        content.append("Your order has been received and we are working on processing it !")
                .append("\n\n")
                .append(buildCartToString(cart))
                .append("\nThank you for your preference !");
        message.setText(content.toString());
        message.setTo(cart.cart().getFirst().email());
        javaMailSender.send(message);
    }

    public void orderRejectedEmail(RejectOrderDTO rejectOrderDTO) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setSubject("Order Canceled");
        StringBuilder content = new StringBuilder();
        content.append("Some products were unavailable at the time of the request so the order was canceled ! Try again later !")
                .append("\n")
                .append("Unavailable products:")
                .append("\n")
                .append(buildUnavailableToString(rejectOrderDTO.unavailable()))
                .append("\n")
                .append("Sorry for the inconvenience !");
        message.setTo(rejectOrderDTO.email());
        message.setText(content.toString());
        javaMailSender.send(message);
    }

    private String buildOrderToString(OrderDTO orderDTO) {
        StringBuilder order = new StringBuilder();
        for(OrderProduct product: orderDTO.order()){
            order.append("\t")
                    .append(product.productName())
                    .append(" - ")
                    .append(product.price())
                    .append(" x ")
                    .append(product.amount())
                    .append("\n");
        }
        return order.toString();
    }

    private String buildUnavailableToString(List<String> unavailable) {
        StringBuilder unavailableBuilder = new StringBuilder();
        for(String s: unavailable){
            unavailableBuilder.append("\t")
                    .append(s)
                    .append("\n");
        }
        return unavailableBuilder.toString();
    }

    private String buildCartToString(CartDTO cartDTO) {
        StringBuilder cartBuilder = new StringBuilder();
        for(CartProduct cartProduct : cartDTO.cart()){
            cartBuilder.append("\t")
                    .append(cartProduct.productName())
                    .append(" - ")
                    .append(cartProduct.amount())
                    .append("\n");
        }
        return cartBuilder.toString();
    }
}
