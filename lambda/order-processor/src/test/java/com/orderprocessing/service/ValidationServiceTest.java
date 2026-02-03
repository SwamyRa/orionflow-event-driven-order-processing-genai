package com.orderprocessing.service;

import com.orderprocessing.model.Address;
import com.orderprocessing.model.Order;
import com.orderprocessing.model.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {
    
    private ValidationService validationService;
    
    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }
    
    @Test
    void testValidateOrder_ValidOrder() {
        // Given
        Order order = createValidOrder();
        
        // When
        List<String> errors = validationService.validateOrder(order);
        
        // Then
        assertTrue(errors.isEmpty());
    }
    
    @Test
    void testValidateOrder_InvalidEmail() {
        // Given
        Order order = createValidOrder();
        order.setCustomerEmail("invalid-email");
        
        // When
        List<String> errors = validationService.validateOrder(order);
        
        // Then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("email")));
    }
    
    @Test
    void testValidateOrder_NegativeAmount() {
        // Given
        Order order = createValidOrder();
        order.setTotalAmount(-100.0);
        
        // When
        List<String> errors = validationService.validateOrder(order);
        
        // Then
        assertFalse(errors.isEmpty());
    }
    
    private Order createValidOrder() {
        Order order = new Order();
        order.setOrderId("ORD-001");
        order.setCustomerId("CUST-001");
        order.setCustomerEmail("test@example.com");
        order.setTotalAmount(100.0);
        order.setPaymentMethod("CREDIT_CARD");
        
        List<OrderItem> items = new ArrayList<>();
        OrderItem item = new OrderItem();
        item.setProductId("PROD-001");
        item.setName("Test Product");
        item.setQuantity(1);
        item.setPrice(100.0);
        items.add(item);
        order.setItems(items);
        
        Address address = new Address();
        address.setStreet("123 Main St");
        address.setCity("Test City");
        address.setCountry("USA");
        order.setShippingAddress(address);
        
        return order;
    }
}
