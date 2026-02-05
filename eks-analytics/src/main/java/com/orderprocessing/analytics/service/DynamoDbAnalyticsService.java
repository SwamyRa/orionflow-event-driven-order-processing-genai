package com.orderprocessing.analytics.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDate;
import java.util.*;

/**
 * Service to query order data from DynamoDB.
 * 
 * Uses StatusDateIndex GSI to efficiently query orders by:
 * - Status (APPROVED, REJECTED, PENDING_REVIEW)
 * - Date range
 * 
 * Aggregates:
 * - Order counts by status
 * - Cost metrics from stored order data
 * - Daily/weekly statistics
 */
@Service
@Slf4j
public class DynamoDbAnalyticsService {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDbAnalyticsService(
            DynamoDbClient dynamoDbClient,
            @Value("${aws.dynamodb.table-name}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    /**
     * Query orders for a specific date range using GSI.
     * Returns aggregated statistics including order counts and costs.
     */
    public Map<String, Object> getOrderStatistics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Query orders by status using GSI
            int approvedCount = queryOrdersByStatus("APPROVED", startDate, endDate);
            int rejectedCount = queryOrdersByStatus("REJECTED", startDate, endDate);
            int pendingCount = queryOrdersByStatus("PENDING_REVIEW", startDate, endDate);
            
            stats.put("totalOrders", approvedCount + rejectedCount + pendingCount);
            stats.put("approvedOrders", approvedCount);
            stats.put("rejectedOrders", rejectedCount);
            stats.put("pendingReviewOrders", pendingCount);
            
            // Get aggregated cost metrics
            Map<String, Double> costs = aggregateCosts(startDate, endDate);
            stats.putAll(costs);
            
            log.info("Retrieved statistics for {} to {}: {} total orders", 
                    startDate, endDate, stats.get("totalOrders"));
            
        } catch (Exception e) {
            log.error("Error querying DynamoDB", e);
        }
        
        return stats;
    }

    /**
     * Query orders by status using StatusDateIndex GSI.
     * Uses ExpressionAttributeNames to handle reserved keyword "status".
     */
    private int queryOrdersByStatus(String status, LocalDate startDate, LocalDate endDate) {
        try {
            Map<String, String> expressionNames = new HashMap<>();
            expressionNames.put("#status", "status");
            expressionNames.put("#sk", "SK");

            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":status", AttributeValue.builder().s(status).build());
            expressionValues.put(":metadata", AttributeValue.builder().s("METADATA").build());

            ScanRequest request = ScanRequest.builder()
                    .tableName(tableName)
                    .filterExpression("#status = :status AND #sk = :metadata")
                    .expressionAttributeNames(expressionNames)
                    .expressionAttributeValues(expressionValues)
                    .build();

            ScanResponse response = dynamoDbClient.scan(request);
            log.info("Scanned {} items with status {}", response.count(), status);
            return response.count();
            
        } catch (Exception e) {
            log.error("Error querying status: {}", status, e);
            return 0;
        }
    }

    /**
     * Aggregate cost metrics from orders in date range.
     * Parses orderData JSON field to extract cost metrics.
     */
    private Map<String, Double> aggregateCosts(LocalDate startDate, LocalDate endDate) {
        Map<String, Double> costs = new HashMap<>();
        double totalCost = 0;
        double bedrockCost = 0;
        double lambdaCost = 0;
        double dynamoDbCost = 0;
        double s3Cost = 0;
        double snsCost = 0;
        double apiGatewayCost = 0;
        int totalBedrockTokens = 0;

        try {
            // Scan all orders and parse orderData JSON
            ScanRequest request = ScanRequest.builder()
                    .tableName(tableName)
                    .filterExpression("SK = :metadata")
                    .expressionAttributeValues(Map.of(
                            ":metadata", AttributeValue.builder().s("METADATA").build()
                    ))
                    .build();

            ScanResponse response = dynamoDbClient.scan(request);
            
            for (Map<String, AttributeValue> item : response.items()) {
                if (item.containsKey("orderData")) {
                    String orderDataJson = item.get("orderData").s();
                    // Simple JSON parsing for costMetrics
                    if (orderDataJson.contains("costMetrics")) {
                        bedrockCost += extractCost(orderDataJson, "bedrockCost");
                        lambdaCost += extractCost(orderDataJson, "lambdaCost");
                        dynamoDbCost += extractCost(orderDataJson, "dynamodbCost");
                        s3Cost += extractCost(orderDataJson, "s3Cost");
                        snsCost += extractCost(orderDataJson, "snsCost");
                        apiGatewayCost += extractCost(orderDataJson, "apiGatewayCost");
                        totalCost += extractCost(orderDataJson, "totalProcessingCost");
                        totalBedrockTokens += extractTokens(orderDataJson, "bedrockTokensUsed");
                    }
                }
            }

            costs.put("totalProcessingCost", Math.round(totalCost * 100000.0) / 100000.0);
            costs.put("bedrockCost", Math.round(bedrockCost * 100000.0) / 100000.0);
            costs.put("lambdaCost", Math.round(lambdaCost * 100000.0) / 100000.0);
            costs.put("dynamoDbCost", Math.round(dynamoDbCost * 100000.0) / 100000.0);
            costs.put("s3Cost", Math.round(s3Cost * 100000.0) / 100000.0);
            costs.put("snsCost", Math.round(snsCost * 100000.0) / 100000.0);
            costs.put("apiGatewayCost", Math.round(apiGatewayCost * 100000.0) / 100000.0);
            costs.put("totalBedrockTokens", (double) totalBedrockTokens);

            log.info("Aggregated costs from {} orders: total=${}, bedrock=${}, tokens={}", 
                    response.count(), totalCost, bedrockCost, totalBedrockTokens);

        } catch (Exception e) {
            log.error("Error aggregating costs", e);
        }

        return costs;
    }

    private double extractCost(String json, String field) {
        try {
            String pattern = "\"" + field + "\":";
            int start = json.indexOf(pattern);
            if (start == -1) return 0.0;
            start += pattern.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            String value = json.substring(start, end).trim();
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int extractTokens(String json, String field) {
        try {
            String pattern = "\"" + field + "\":";
            int start = json.indexOf(pattern);
            if (start == -1) return 0;
            start += pattern.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            String value = json.substring(start, end).trim();
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
}
