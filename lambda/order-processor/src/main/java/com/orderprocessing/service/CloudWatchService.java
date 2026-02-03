package com.orderprocessing.service;

import com.orderprocessing.model.CostMetrics;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for publishing custom FinOps metrics to CloudWatch.
 * 
 * Namespace: OrderProcessing/FinOps
 * 
 * Metrics published:
 * - OrderProcessingCost: Total cost per order
 * - BedrockTokens: AI tokens used
 * - LambdaDuration: Execution time
 * - BedrockCost: AI-specific cost
 * 
 * These metrics enable:
 * - Real-time cost dashboards
 * - Cost anomaly detection
 * - Budget alerts
 * - EKS analytics layer queries
 */
@Slf4j
public class CloudWatchService {
    
    private final CloudWatchClient cloudWatchClient;
    private static final String NAMESPACE = "OrderProcessing/FinOps";
    
    public CloudWatchService(CloudWatchClient cloudWatchClient) {
        this.cloudWatchClient = cloudWatchClient;
    }
    
    /**
     * Publishes FinOps metrics to CloudWatch for real-time cost tracking.
     * Metrics are dimensioned by OrderStatus for granular analysis.
     * 
     * @param orderId Order identifier for logging
     * @param status Order status (APPROVED, REJECTED, etc.)
     * @param costMetrics Cost breakdown to publish
     */
    public void publishMetrics(String orderId, String status, CostMetrics costMetrics) {
        try {
            List<MetricDatum> metrics = new ArrayList<>();
            
            Dimension statusDimension = Dimension.builder()
                    .name("OrderStatus")
                    .value(status)
                    .build();
            
            // Total processing cost
            metrics.add(MetricDatum.builder()
                    .metricName("OrderProcessingCost")
                    .value(costMetrics.getTotalProcessingCost())
                    .unit(StandardUnit.NONE)
                    .timestamp(Instant.now())
                    .dimensions(statusDimension)
                    .build());
            
            // Bedrock tokens
            metrics.add(MetricDatum.builder()
                    .metricName("BedrockTokens")
                    .value(costMetrics.getBedrockTokensUsed().doubleValue())
                    .unit(StandardUnit.COUNT)
                    .timestamp(Instant.now())
                    .dimensions(statusDimension)
                    .build());
            
            // Lambda duration
            metrics.add(MetricDatum.builder()
                    .metricName("LambdaDuration")
                    .value(costMetrics.getLambdaDurationMs().doubleValue())
                    .unit(StandardUnit.MILLISECONDS)
                    .timestamp(Instant.now())
                    .dimensions(statusDimension)
                    .build());
            
            // Bedrock cost
            metrics.add(MetricDatum.builder()
                    .metricName("BedrockCost")
                    .value(costMetrics.getBedrockCost())
                    .unit(StandardUnit.NONE)
                    .timestamp(Instant.now())
                    .dimensions(statusDimension)
                    .build());
            
            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(NAMESPACE)
                    .metricData(metrics)
                    .build();
            
            cloudWatchClient.putMetricData(request);
            
            log.info("CloudWatch metrics published for order: {}", orderId);
            
        } catch (Exception e) {
            log.error("Error publishing CloudWatch metrics for order: {}", orderId, e);
        }
    }
}
