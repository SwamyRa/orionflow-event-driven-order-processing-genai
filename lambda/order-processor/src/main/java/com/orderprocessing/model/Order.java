package com.orderprocessing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orderprocessing.constants.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("customerId")
    private String customerId;
    
    @JsonProperty("customerEmail")
    private String customerEmail;
    
    @JsonProperty("customerType")
    private String customerType;
    
    @JsonProperty("orderHistory")
    private Integer orderHistory;
    
    @JsonProperty("items")
    private List<OrderItem> items;
    
    @JsonProperty("totalAmount")
    private Double totalAmount;
    
    @JsonProperty("shippingAddress")
    private Address shippingAddress;
    
    @JsonProperty("billingAddress")
    private Address billingAddress;
    
    @JsonProperty("paymentMethod")
    private String paymentMethod;
    
    @JsonProperty("cardLast4")
    private String cardLast4;
    
    // Purchase Order Number (for business customers)
    @JsonProperty("poNumber")
    private String purchaseOrderNumber;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("status")
    private OrderStatus status;
    
    @JsonProperty("aiScore")
    private Double aiScore;
    
    @JsonProperty("rejectionReasons")
    private List<String> rejectionReasons;
    
    @JsonProperty("costMetrics")
    private CostMetrics costMetrics;
    
    @JsonProperty("processedAt")
    private Instant processedAt;
}
