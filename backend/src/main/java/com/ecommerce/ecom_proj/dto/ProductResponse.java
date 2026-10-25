package com.ecommerce.ecom_proj.dto;

import com.ecommerce.ecom_proj.model.Product;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String brand,
        BigDecimal price,
        String category,
        LocalDate releaseDate,
        boolean productAvailable,
        int stockQuantity
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getBrand(), p.getPrice(),
                p.getCategory(), p.getReleaseDate(), p.isProductAvailable(), p.getStockQuantity());
    }
}
