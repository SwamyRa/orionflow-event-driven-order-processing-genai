package com.orderprocessing.service;

import com.orderprocessing.model.Order;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for validating order data before processing.
 * Validates orders BEFORE calling Bedrock to save costs on invalid requests.
 * 
 * Validation includes:
 * - Required fields (orderId, customerId, email)
 * - Email format
 * - Order items and amounts
 * - Shipping address completeness
 * - Payment method
 */
@Slf4j
public class ValidationService {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    /**
     * Validates all required order fields and business rules.
     * 
     * @param order Order object to validate
     * @return List of validation error messages (empty if valid)
     */
    public List<String> validateOrder(Order order) {
        List<String> errors = new ArrayList<>();
        
        if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
            errors.add("Order ID is required");
        }
        
        if (order.getCustomerId() == null || order.getCustomerId().isEmpty()) {
            errors.add("Customer ID is required");
        }
        
        if (order.getCustomerEmail() == null || !isValidEmail(order.getCustomerEmail())) {
            errors.add("Valid email is required");
        }
        
        if (order.getItems() == null || order.getItems().isEmpty()) {
            errors.add("Order must have at least one item");
        }
        
        if (order.getTotalAmount() == null || order.getTotalAmount() <= 0) {
            errors.add("Total amount must be greater than zero");
        }
        
        if (order.getShippingAddress() == null) {
            errors.add("Shipping address is required");
        } else {
            if (order.getShippingAddress().getStreet() == null || order.getShippingAddress().getStreet().isEmpty()) {
                errors.add("Shipping street is required");
            }
            if (order.getShippingAddress().getCity() == null || order.getShippingAddress().getCity().isEmpty()) {
                errors.add("Shipping city is required");
            }
            if (order.getShippingAddress().getCountry() == null || order.getShippingAddress().getCountry().isEmpty()) {
                errors.add("Shipping country is required");
            }
        }
        
        if (order.getPaymentMethod() == null || order.getPaymentMethod().isEmpty()) {
            errors.add("Payment method is required");
        }
        
        if (errors.isEmpty()) {
            log.info("Order validation passed for orderId: {}", order.getOrderId());
        } else {
            log.warn("Order validation failed for orderId: {} with {} errors", order.getOrderId(), errors.size());
        }
        
        return errors;
    }
    
    /**
     * Validates email format using regex pattern.
     */
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
}
