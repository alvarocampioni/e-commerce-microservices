package com.ms.cart_service.dto;

import com.ms.cart_service.model.CartProduct;

import java.io.Serializable;
import java.util.List;

public record CartDTO (List<CartProduct> cart) implements Serializable {
}
