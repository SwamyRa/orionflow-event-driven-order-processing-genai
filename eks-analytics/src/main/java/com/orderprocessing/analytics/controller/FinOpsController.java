package com.orderprocessing.analytics.controller;

import com.orderprocessing.analytics.model.FinOpsReport;
import com.orderprocessing.analytics.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * REST controller for FinOps metrics and cost data.
 * 
 * Provides on-demand access to:
 * - Order statistics by date range
 * - Actual AWS costs from Cost Explorer
 * - 30-day cost forecasts
 * - Complete FinOps reports
 * 
 * No scheduled CronJobs - all reports generated on API call.
 * This saves costs by not keeping pods running 24/7.
 */
@RestController
@RequestMapping("/api/finops")
@Slf4j
public class FinOpsController {

    private final DynamoDbAnalyticsService dynamoDbService;
    private final CloudWatchAnalyticsService cloudWatchService;
    private final CostExplorerService costExplorerService;
    private final DailyReportService reportService;

    public FinOpsController(
            DynamoDbAnalyticsService dynamoDbService,
            CloudWatchAnalyticsService cloudWatchService,
            CostExplorerService costExplorerService,
            DailyReportService reportService) {
        this.dynamoDbService = dynamoDbService;
        this.cloudWatchService = cloudWatchService;
        this.costExplorerService = costExplorerService;
        this.reportService = reportService;
    }

    /**
     * Get order metrics for date range.
     * 
     * @param startDate Start date (YYYY-MM-DD), defaults to 7 days ago
     * @param endDate End date (YYYY-MM-DD), defaults to today
     * @return Order statistics including counts and costs
     */
    @GetMapping("/metrics")
    public Map<String, Object> getMetrics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(7);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        return dynamoDbService.getOrderStatistics(start, end);
    }

    /**
     * Get actual AWS costs from Cost Explorer.
     * 
     * @param startDate Start date (YYYY-MM-DD)
     * @param endDate End date (YYYY-MM-DD)
     * @return Actual costs by service
     */
    @GetMapping("/costs")
    public Map<String, Double> getCosts(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(7);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        return costExplorerService.getActualCosts(start, end);
    }

    /**
     * Get 30-day cost forecast.
     * 
     * @return Forecasted monthly cost
     */
    @GetMapping("/forecast")
    public Map<String, Double> getForecast() {
        return Map.of("forecastedMonthlyCost", costExplorerService.getCostForecast());
    }

    /**
     * Generate complete FinOps report on-demand.
     * 
     * @param startDate Start date (YYYY-MM-DD), defaults to yesterday
     * @param endDate End date (YYYY-MM-DD), defaults to yesterday
     * @return Complete FinOps report with all metrics
     */
    @GetMapping("/report")
    public FinOpsReport generateReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now().minusDays(1);

        return reportService.generateReport(start, end);
    }
}
