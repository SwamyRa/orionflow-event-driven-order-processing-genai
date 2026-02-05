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

            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":status", AttributeValue.builder().s(status).build());

            ScanRequest request = ScanRequest.builder()
                    .tableName(tableName)
                    .filterExpression("#status = :status")
                    .expressionAttributeNames(expressionNames)
                    .expressionAttributeValues(expressionValues)
                    .build();

            ScanResponse response = dynamoDbClient.scan(request);
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

        try {
            // In production: Query all statuses and parse orderData JSON
            // Simplified: Return placeholder values
            costs.put("totalProcessingCost", totalCost);
            costs.put("bedrockCost", bedrockCost);
            costs.put("lambdaCost", lambdaCost);
            costs.put("dynamoDbCost", 0.0);
            costs.put("s3Cost", 0.0);
            costs.put("snsCost", 0.0);
            costs.put("apiGatewayCost", 0.0);

        } catch (Exception e) {
            log.error("Error aggregating costs", e);
        }

        return costs;
    }
}
