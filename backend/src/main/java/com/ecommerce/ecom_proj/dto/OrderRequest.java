package com.ecommerce.ecom_proj.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "An order needs at least one item")
        List<@Valid OrderItemRequest> items
) {
}
