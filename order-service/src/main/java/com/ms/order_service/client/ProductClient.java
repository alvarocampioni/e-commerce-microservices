package com.ms.order_service.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import java.math.BigDecimal;

public interface ProductClient {

    @GetExchange("/api/product/{id}/available")
    boolean isAvailable(@PathVariable String id, @RequestParam int amount);

    @GetExchange("/api/product/{id}/price")
    BigDecimal getPrice(@PathVariable String id);


}
