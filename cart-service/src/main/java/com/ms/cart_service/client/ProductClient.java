package com.ms.cart_service.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

public interface ProductClient {

    @GetExchange("/api/product/{productId}/available")
    boolean isAvailable(@PathVariable String productId, @RequestParam int amount);

    @GetExchange("/api/product/{productId}/name")
    String getName(@PathVariable String productId);
}
