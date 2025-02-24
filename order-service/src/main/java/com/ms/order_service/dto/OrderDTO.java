package com.ms.order_service.dto;

import com.ms.order_service.model.OrderProduct;

import java.io.Serializable;
import java.util.List;

public record OrderDTO (List<OrderProduct> order) implements Serializable {
}
