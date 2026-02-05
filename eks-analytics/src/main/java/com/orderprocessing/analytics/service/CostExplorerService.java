package com.orderprocessing.analytics.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to query AWS Cost Explorer for actual costs.
 * 
 * Compares estimated costs (from Lambda calculations) vs actual AWS bills.
 * 
 * Provides:
 * - Actual costs by service (Lambda, DynamoDB, S3, Bedrock)
 * - Cost forecasts for next 30 days
 * - Variance analysis for FinOps accuracy
 */
@Service
@Slf4j
public class CostExplorerService {

    private final CostExplorerClient costExplorerClient;

    public CostExplorerService(CostExplorerClient costExplorerClient) {
        this.costExplorerClient = costExplorerClient;
    }

    /**
     * Get actual AWS costs for services in date range.
     * Groups costs by AWS service for comparison with estimates.
     * 
     * Note: Cost Explorer requires end date > start date, so we ensure minimum 1-day range.
     */
    public Map<String, Double> getActualCosts(LocalDate startDate, LocalDate endDate) {
        Map<String, Double> costs = new HashMap<>();

        try {
            // Cost Explorer requires end date to be strictly after start date
            // If startDate == endDate, we need to add at least 1 day to end
            LocalDate adjustedEndDate = endDate.isBefore(startDate.plusDays(1)) 
                    ? startDate.plusDays(1) 
                    : endDate.plusDays(1);
            
            DateInterval dateInterval = DateInterval.builder()
                    .start(startDate.toString())
                    .end(adjustedEndDate.toString())
                    .build();

            GetCostAndUsageRequest request = GetCostAndUsageRequest.builder()
                    .timePeriod(dateInterval)
                    .granularity(Granularity.DAILY)
                    .metrics("UnblendedCost")
                    .groupBy(
                            GroupDefinition.builder()
                                    .type(GroupDefinitionType.DIMENSION)
                                    .key("SERVICE")
                                    .build()
                    )
                    .build();

            GetCostAndUsageResponse response = costExplorerClient.getCostAndUsage(request);

            // Aggregate costs by service
            for (ResultByTime result : response.resultsByTime()) {
                for (Group group : result.groups()) {
                    String service = group.keys().get(0);
                    double cost = Double.parseDouble(group.metrics().get("UnblendedCost").amount());

                    if (service.contains("Lambda")) {
                        costs.put("actualLambdaCost", costs.getOrDefault("actualLambdaCost", 0.0) + cost);
                    } else if (service.contains("DynamoDB")) {
                        costs.put("actualDynamoDbCost", costs.getOrDefault("actualDynamoDbCost", 0.0) + cost);
                    } else if (service.contains("S3")) {
                        costs.put("actualS3Cost", costs.getOrDefault("actualS3Cost", 0.0) + cost);
                    } else if (service.contains("Bedrock")) {
                        costs.put("actualBedrockCost", costs.getOrDefault("actualBedrockCost", 0.0) + cost);
                    } else if (service.contains("SNS") || service.contains("Simple Notification")) {
                        costs.put("actualSnsCost", costs.getOrDefault("actualSnsCost", 0.0) + cost);
                    } else if (service.contains("API Gateway")) {
                        costs.put("actualApiGatewayCost", costs.getOrDefault("actualApiGatewayCost", 0.0) + cost);
                    }
                }
            }

            // Ensure all services are present with 5 decimal places
            costs.putIfAbsent("actualLambdaCost", 0.0);
            costs.putIfAbsent("actualDynamoDbCost", 0.0);
            costs.putIfAbsent("actualS3Cost", 0.0);
            costs.putIfAbsent("actualBedrockCost", 0.0);
            costs.putIfAbsent("actualSnsCost", 0.0);
            costs.putIfAbsent("actualApiGatewayCost", 0.0);

            // Round to 5 decimal places
            costs.replaceAll((k, v) -> Math.round(v * 100000.0) / 100000.0);

            log.info("Retrieved actual costs from Cost Explorer: {}", costs);

        } catch (Exception e) {
            log.error("Error querying Cost Explorer", e);
        }

        return costs;
    }

    /**
     * Get cost forecast for next 30 days.
     * Uses AWS machine learning models for prediction.
     */
    public double getCostForecast() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate futureDate = today.plusDays(30);

            DateInterval dateInterval = DateInterval.builder()
                    .start(today.toString())
                    .end(futureDate.toString())
                    .build();

            GetCostForecastRequest request = GetCostForecastRequest.builder()
                    .timePeriod(dateInterval)
                    .metric(Metric.UNBLENDED_COST)
                    .granularity(Granularity.MONTHLY)
                    .build();

            GetCostForecastResponse response = costExplorerClient.getCostForecast(request);
            return Double.parseDouble(response.total().amount());

        } catch (Exception e) {
            log.error("Error getting cost forecast", e);
            return 0.0;
        }
    }
}
