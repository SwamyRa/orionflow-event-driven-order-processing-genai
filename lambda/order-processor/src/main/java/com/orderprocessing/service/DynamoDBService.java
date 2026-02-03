package com.orderprocessing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orderprocessing.model.Order;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for persisting orders to DynamoDB.
 * 
 * Table design:
 * - PK: ORDER#<orderId>
 * - SK: METADATA
 * - GSI: StatusDateIndex (status + createdAt) for querying by status
 * 
 * Stores both individual fields and complete order JSON for flexibility.
 */
@Slf4j
public class DynamoDBService {
    
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final ObjectMapper objectMapper;
    
    public DynamoDBService(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Saves order to DynamoDB with PK/SK pattern and GSI for status queries.
     * 
     * @param order Order to save
     * @throws RuntimeException if save fails
     */
    public void saveOrder(Order order) {
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            
            item.put("PK", AttributeValue.builder().s("ORDER#" + order.getOrderId()).build());
            item.put("SK", AttributeValue.builder().s("METADATA").build());
            item.put("orderId", AttributeValue.builder().s(order.getOrderId()).build());
            item.put("customerId", AttributeValue.builder().s(order.getCustomerId()).build());
            item.put("customerEmail", AttributeValue.builder().s(order.getCustomerEmail()).build());
            item.put("status", AttributeValue.builder().s(order.getStatus().name()).build());
            item.put("totalAmount", AttributeValue.builder().n(String.valueOf(order.getTotalAmount())).build());
            item.put("timestamp", AttributeValue.builder().s(order.getTimestamp().toString()).build());
            item.put("processedAt", AttributeValue.builder().s(order.getProcessedAt().toString()).build());
            
            if (order.getAiScore() != null) {
                item.put("aiScore", AttributeValue.builder().n(String.valueOf(order.getAiScore())).build());
            }
            
            // Store entire order as JSON for easy retrieval
            String orderJson = objectMapper.writeValueAsString(order);
            item.put("orderData", AttributeValue.builder().s(orderJson).build());
            
            // GSI for querying by status and date
            item.put("GSI1PK", AttributeValue.builder().s("STATUS#" + order.getStatus().name()).build());
            item.put("GSI1SK", AttributeValue.builder().s(order.getTimestamp().toString()).build());
            
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();
            
            dynamoDbClient.putItem(request);
            
            log.info("Order saved to DynamoDB: {}", order.getOrderId());
            
        } catch (Exception e) {
            log.error("Error saving order to DynamoDB: {}", order.getOrderId(), e);
            throw new RuntimeException("Failed to save order", e);
        }
    }
}
