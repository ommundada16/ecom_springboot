package com.ecommerce.ecom_proj.dto;

import com.ecommerce.ecom_proj.model.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(Long productId, String productName, BigDecimal unitPrice, int quantity,
                                BigDecimal lineTotal) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct() == null ? null : item.getProduct().getId(),
                item.getProductName(), item.getUnitPrice(), item.getQuantity(), item.getLineTotal());
    }
}
