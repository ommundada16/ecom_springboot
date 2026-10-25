package com.ecommerce.ecom_proj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        @Size(max = 100, message = "Brand must be at most 100 characters")
        String brand,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than zero")
        BigDecimal price,

        @Size(max = 50, message = "Category must be at most 50 characters")
        String category,

        LocalDate releaseDate,

        boolean productAvailable,

        @NotNull(message = "Stock quantity is required")
        @PositiveOrZero(message = "Stock quantity cannot be negative")
        Integer stockQuantity
) {
}
