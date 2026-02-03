package com.orderprocessing.analytics.model;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * Daily FinOps report model.
 * 
 * Contains:
 * - Order statistics (total, approved, rejected)
 * - Estimated costs (from Lambda cost calculations)
 * - Actual costs (from AWS Cost Explorer)
 * - Variance analysis (estimated vs actual)
 * - Cost trends and forecasts
 * - Optimization recommendations
 */
@Data
public class FinOpsReport {
    
    // Report metadata
    private LocalDate reportDate;
    private String period; // "Daily" or "Weekly"
    
    // Order statistics
    private int totalOrders;
    private int approvedOrders;
    private int rejectedOrders;
    private int pendingReviewOrders;
    
    // Estimated costs (from Lambda calculations stored in DynamoDB)
    private double totalProcessingCost;
    private double bedrockCost;
    private double lambdaCost;
    private double dynamoDbCost;
    private double s3Cost;
    private double snsCost;
    private double apiGatewayCost;
    
    // Actual costs (from AWS Cost Explorer API)
    private double actualLambdaCost;
    private double actualDynamoDbCost;
    private double actualS3Cost;
    private double actualBedrockCost;
    
    // Variance analysis
    private double estimatedVsActualVariance; // Percentage difference
    
    // Trends (compared to previous period)
    private double costTrend; // % change
    private double orderTrend; // % change
    
    // Recommendations
    private List<String> optimizationRecommendations;
    
    // Forecast
    private double forecastedMonthlyCost;
}
