package com.ms.product_service.dto;

import java.util.List;

public record OrderDTO(List<OrderProduct> order) {
}
