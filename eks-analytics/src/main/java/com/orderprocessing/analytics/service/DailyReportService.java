package com.orderprocessing.analytics.service;

import com.orderprocessing.analytics.model.FinOpsReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

/**
 * Service to generate FinOps reports on-demand.
 * 
 * No scheduled CronJob - reports are generated via API call.
 * This saves costs by not keeping pods running 24/7.
 * 
 * Aggregates data from:
 * - DynamoDB: Order statistics
 * - CloudWatch: Custom metrics
 * - Cost Explorer: Actual AWS costs
 */
@Service
@Slf4j
public class DailyReportService {

    private final DynamoDbAnalyticsService dynamoDbService;
    private final CloudWatchAnalyticsService cloudWatchService;
    private final CostExplorerService costExplorerService;

    public DailyReportService(
            DynamoDbAnalyticsService dynamoDbService,
            CloudWatchAnalyticsService cloudWatchService,
            CostExplorerService costExplorerService) {
        this.dynamoDbService = dynamoDbService;
        this.cloudWatchService = cloudWatchService;
        this.costExplorerService = costExplorerService;
    }

    /**
     * Generates FinOps report for specified date range.
     * Called via REST API endpoint.
     * 
     * @param startDate Start date for report
     * @param endDate End date for report
     * @return Complete FinOps report with costs and statistics
     */
    public FinOpsReport generateReport(LocalDate startDate, LocalDate endDate) {
        log.info("Generating FinOps report for {} to {}", startDate, endDate);

        FinOpsReport report = new FinOpsReport();
        report.setReportDate(endDate);
        report.setPeriod(startDate.equals(endDate) ? "Daily" : "Custom");

        // Get order statistics from DynamoDB
        Map<String, Object> orderStats = dynamoDbService.getOrderStatistics(startDate, endDate);
        report.setTotalOrders((Integer) orderStats.get("totalOrders"));
        report.setApprovedOrders((Integer) orderStats.get("approvedOrders"));
        report.setRejectedOrders((Integer) orderStats.get("rejectedOrders"));
        report.setPendingReviewOrders((Integer) orderStats.get("pendingReviewOrders"));

        // Get CloudWatch metrics
        Map<String, Double> cloudWatchMetrics = cloudWatchService.getMetricStatistics(startDate, endDate);
        report.setTotalProcessingCost(cloudWatchMetrics.getOrDefault("avgOrderCost", 0.0));

        // Get actual costs from Cost Explorer
        Map<String, Double> actualCosts = costExplorerService.getActualCosts(startDate, endDate);
        report.setActualLambdaCost(actualCosts.getOrDefault("actualLambdaCost", 0.0));
        report.setActualDynamoDbCost(actualCosts.getOrDefault("actualDynamoDbCost", 0.0));
        report.setActualS3Cost(actualCosts.getOrDefault("actualS3Cost", 0.0));
        report.setActualBedrockCost(actualCosts.getOrDefault("actualBedrockCost", 0.0));

        // Get cost forecast
        report.setForecastedMonthlyCost(costExplorerService.getCostForecast());

        // Generate recommendations
        report.setOptimizationRecommendations(generateRecommendations(report));

        log.info("Report generated: {} orders, ${} total cost", 
                report.getTotalOrders(), report.getTotalProcessingCost());

        return report;
    }

    /**
     * Generate cost optimization recommendations based on report data.
     */
    private java.util.List<String> generateRecommendations(FinOpsReport report) {
        java.util.List<String> recommendations = new ArrayList<>();

        if (report.getTotalOrders() > 0) {
            double costPerOrder = report.getTotalProcessingCost() / report.getTotalOrders();
            if (costPerOrder > 0.005) {
                recommendations.add("High cost per order detected. Review Bedrock token usage.");
            }
        }

        if (report.getActualBedrockCost() > report.getBedrockCost() * 1.2) {
            recommendations.add("Bedrock costs 20% higher than estimated. Update cost calculator.");
        }

        return recommendations;
    }
}
