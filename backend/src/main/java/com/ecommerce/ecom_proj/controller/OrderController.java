package com.ecommerce.ecom_proj.controller;

import com.ecommerce.ecom_proj.dto.OrderRequest;
import com.ecommerce.ecom_proj.dto.OrderResponse;
import com.ecommerce.ecom_proj.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Checkout and order history for the logged-in user")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place an order",
            description = "Atomically validates stock, decrements it and records the order. 409 if stock is insufficient.")
    public OrderResponse placeOrder(Authentication authentication, @Valid @RequestBody OrderRequest request) {
        return orderService.placeOrder(authentication.getName(), request);
    }

    @GetMapping
    @Operation(summary = "List my orders, newest first")
    public List<OrderResponse> myOrders(Authentication authentication) {
        return orderService.getOrdersForUser(authentication.getName());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my orders (admins can read any order)")
    public OrderResponse getOrder(@PathVariable Long id, Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return orderService.getOrder(id, authentication.getName(), admin);
    }
}
