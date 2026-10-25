package com.ecommerce.ecom_proj.exception;

public class InsufficientStockException extends ConflictException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
