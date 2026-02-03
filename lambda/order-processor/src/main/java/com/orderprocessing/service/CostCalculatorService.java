package com.orderprocessing.service;

import com.orderprocessing.model.CostMetrics;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for calculating real-time AWS service costs for order processing.
 * Implements FinOps best practices by tracking costs at transaction level.
 * 
 * Pricing is based on AWS us-east-1 region rates (as of 2024).
 */
@Slf4j
public class CostCalculatorService {
    
    // AWS Pricing constants
    private static final double BEDROCK_COST_PER_1K_TOKENS = 0.003;
    private static final double LAMBDA_COST_PER_GB_SECOND = 0.0000166667;
    private static final double DYNAMODB_WRITE_COST_PER_MILLION = 1.25;
    private static final double S3_PUT_COST_PER_1K_REQUESTS = 0.005;
    private static final double SNS_COST_PER_MILLION = 0.50;
    private static final double API_GATEWAY_COST_PER_MILLION = 3.50;
    
    /**
     * Calculates comprehensive cost metrics for a single order processing transaction.
     * 
     * @param durationMs Lambda execution duration in milliseconds
     * @param memoryMB Lambda memory allocation in MB
     * @param bedrockTokens Total tokens used by Bedrock AI (input + output)
     * @return CostMetrics object with detailed cost breakdown
     */
    public CostMetrics calculateCosts(long durationMs, int memoryMB, int bedrockTokens) {
        CostMetrics metrics = new CostMetrics();
        
        // Bedrock cost
        metrics.setBedrockTokensUsed(bedrockTokens);
        metrics.setBedrockCost(calculateBedrockCost(bedrockTokens));
        
        // Lambda cost
        metrics.setLambdaDurationMs(durationMs);
        metrics.setLambdaCost(calculateLambdaCost(durationMs, memoryMB));
        
        // DynamoDB cost (1 write)
        metrics.setDynamodbWriteUnits(1);
        metrics.setDynamodbCost(DYNAMODB_WRITE_COST_PER_MILLION / 1_000_000);
        
        // S3 cost (1 PUT request)
        metrics.setS3PutRequests(1);
        metrics.setS3Cost(S3_PUT_COST_PER_1K_REQUESTS / 1_000);
        
        // SNS cost (1 notification)
        metrics.setSnsNotifications(1);
        metrics.setSnsCost(SNS_COST_PER_MILLION / 1_000_000);
        
        // API Gateway cost (1 request)
        metrics.setApiGatewayCalls(1);
        metrics.setApiGatewayCost(API_GATEWAY_COST_PER_MILLION / 1_000_000);
        
        // Total cost
        metrics.setTotalProcessingCost(metrics.calculateTotal());
        
        log.info("Calculated costs - Total: ${}, Bedrock: ${}, Lambda: ${}", 
                 metrics.getTotalProcessingCost(), 
                 metrics.getBedrockCost(), 
                 metrics.getLambdaCost());
        
        return metrics;
    }
    
    /**
     * Calculates Bedrock AI cost based on token usage.
     * Claude 3 Sonnet pricing: $0.003 per 1K tokens.
     */
    private double calculateBedrockCost(int tokens) {
        return (tokens / 1000.0) * BEDROCK_COST_PER_1K_TOKENS;
    }
    
    /**
     * Calculates Lambda cost based on GB-seconds.
     * Formula: (Memory in GB) × (Duration in seconds) × $0.0000166667
     */
    private double calculateLambdaCost(long durationMs, int memoryMB) {
        double gbSeconds = (memoryMB / 1024.0) * (durationMs / 1000.0);
        return gbSeconds * LAMBDA_COST_PER_GB_SECOND;
    }
}
