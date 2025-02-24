package com.ms.cart_service.events;

import com.ms.cart_service.service.CartProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class CartEventConsumer {

    private final CartProductService cartProductService;

    @Autowired
    public CartEventConsumer(CartProductService cartProductService) {
        this.cartProductService = cartProductService;
    }

    @KafkaListener(topics = "requested-order", groupId = "request")
    public void receiveOrderRequest(String customerId){
        cartProductService.placeOrder(customerId);
    }

    @KafkaListener(topics = "created-order", groupId = "success")
    public void receiveCartRequest(String customerId){
        cartProductService.deleteCart(customerId);
    }
}
