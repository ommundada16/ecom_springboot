package com.ecommerce.ecom_proj.dto;

public record AuthResponse(String token, Long id, String name, String email, String role) {
}
