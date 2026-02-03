package com.orderprocessing.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application for EKS Analytics Service.
 * 
 * Provides on-demand FinOps reports via REST API.
 * No scheduled CronJobs - saves costs by running only when needed.
 * 
 * Queries data from:
 * - DynamoDB: Order data with cost metrics
 * - CloudWatch: Custom FinOps metrics
 * - AWS Cost Explorer: Actual AWS bills
 * 
 * Deploy as Kubernetes Deployment (not CronJob).
 * Scale to zero when not in use to minimize costs.
 */
@SpringBootApplication
public class AnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsApplication.class, args);
    }
}
