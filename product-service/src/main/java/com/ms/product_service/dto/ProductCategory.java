package com.ms.product_service.dto;

import com.ms.product_service.exception.ResourceNotFoundException;

public enum ProductCategory {
    FOOD,
    TOOL,
    ELECTRONIC,
    CLOTHING;


    public static ProductCategory fromString(String string) {
        for (ProductCategory productCategory : ProductCategory.values()) {
            if (productCategory.toString().equalsIgnoreCase(string)) {
                return productCategory;
            }
        }
        throw new ResourceNotFoundException("Category not found: " + string);
    }
}
