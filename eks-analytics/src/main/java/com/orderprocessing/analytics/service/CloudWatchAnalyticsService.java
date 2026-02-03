package com.orderprocessing.analytics.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to query CloudWatch custom metrics.
 * 
 * Queries metrics from "OrderProcessing/FinOps" namespace:
 * - OrderProcessingCost: Total cost per order
 * - BedrockTokens: AI tokens used per order
 * - LambdaDuration: Execution time per order
 * - BedrockCost: AI-specific cost per order
 * 
 * Returns average values for date range.
 */
@Service
@Slf4j
public class CloudWatchAnalyticsService {

    private final CloudWatchClient cloudWatchClient;
    private static final String NAMESPACE = "OrderProcessing/FinOps";

    public CloudWatchAnalyticsService(CloudWatchClient cloudWatchClient) {
        this.cloudWatchClient = cloudWatchClient;
    }

    /**
     * Get metric statistics for a date range.
     * Returns average values for all FinOps metrics.
     */
    public Map<String, Double> getMetricStatistics(LocalDate startDate, LocalDate endDate) {
        Map<String, Double> metrics = new HashMap<>();

        try {
            Instant startTime = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant endTime = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

            // Get average cost per order
            double avgCost = getMetricAverage("OrderProcessingCost", startTime, endTime);
            metrics.put("avgOrderCost", avgCost);

            // Get average Bedrock tokens
            double avgTokens = getMetricAverage("BedrockTokens", startTime, endTime);
            metrics.put("avgBedrockTokens", avgTokens);

            // Get average Lambda duration
            double avgDuration = getMetricAverage("LambdaDuration", startTime, endTime);
            metrics.put("avgLambdaDuration", avgDuration);

            log.info("Retrieved CloudWatch metrics: avgCost={}, avgTokens={}", avgCost, avgTokens);

        } catch (Exception e) {
            log.error("Error querying CloudWatch metrics", e);
        }

        return metrics;
    }

    /**
     * Get average value for a specific metric.
     */
    private double getMetricAverage(String metricName, Instant startTime, Instant endTime) {
        try {
            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace(NAMESPACE)
                    .metricName(metricName)
                    .startTime(startTime)
                    .endTime(endTime)
                    .period(86400) // 1 day in seconds
                    .statistics(Statistic.AVERAGE)
                    .build();

            GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

            if (!response.datapoints().isEmpty()) {
                return response.datapoints().get(0).average();
            }

        } catch (Exception e) {
            log.error("Error getting metric: {}", metricName, e);
        }

        return 0.0;
    }
}
