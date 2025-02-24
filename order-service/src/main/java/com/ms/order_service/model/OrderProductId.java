package com.ms.order_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderProductId implements Serializable {

    private String id;
    private String customerId;
    private String productId;

}
