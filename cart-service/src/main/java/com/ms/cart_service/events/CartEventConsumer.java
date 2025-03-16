package com.ms.cart_service.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.cart_service.model.Product;
import com.ms.cart_service.service.CartProductService;
import com.ms.cart_service.service.ProductService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class CartEventConsumer {

    private final CartProductService cartProductService;
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    @Autowired
    public CartEventConsumer(CartProductService cartProductService, ProductService productService, ObjectMapper objectMapper) {
        this.cartProductService = cartProductService;
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"created-product", "updated-product", "deleted-product"}, groupId = "cart-products")
    public void receiveProductEvents(ConsumerRecord<String, String> record) {
        try{
            String json = record.value();
            Product product = objectMapper.readValue(json, Product.class);
            String topic = record.topic();
            switch (topic){
                case "created-product":
                    productService.addProduct(product);
                    break;
                case "updated-product":
                    productService.updateProduct(product);
                    break;
                case "deleted-product":
                    productService.deleteProduct(product);
                    break;
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "created-order", groupId = "request")
    public void receiveOrderRequest(String email){
        cartProductService.placeOrder(email);
    }

    @KafkaListener(topics = "user-deleted", groupId = "cart-delete")
    public void receiveUserDeleted(String email){
        cartProductService.deleteCart(email);
    }
}
