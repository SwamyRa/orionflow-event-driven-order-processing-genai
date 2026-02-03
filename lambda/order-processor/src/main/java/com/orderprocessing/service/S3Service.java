package com.orderprocessing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orderprocessing.model.Order;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDate;

/**
 * Service for archiving orders to S3 for long-term storage and compliance.
 * 
 * S3 path structure: <status>/<date>/<orderId>.json
 * Example: approved/2024-01-24/ORD-2024-001.json
 * 
 * Benefits:
 * - 98% cheaper than DynamoDB for historical data
 * - Compliance and audit trail
 * - Foundation for data lake and analytics
 * - Lifecycle policies move to Glacier after 90 days
 */
@Slf4j
public class S3Service {
    
    private final S3Client s3Client;
    private final String bucketName;
    private final ObjectMapper objectMapper;
    
    public S3Service(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Archives order to S3 as pretty-printed JSON.
     * 
     * @param order Order to archive
     * @throws RuntimeException if archive fails
     */
    public void archiveOrder(Order order) {
        try {
            String orderJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(order);
            
            LocalDate date = LocalDate.now();
            String key = String.format("%s/%s/%s.json", 
                    order.getStatus().name().toLowerCase(),
                    date.toString(),
                    order.getOrderId());
            
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/json")
                    .build();
            
            s3Client.putObject(request, RequestBody.fromString(orderJson));
            
            log.info("Order archived to S3: s3://{}/{}", bucketName, key);
            
        } catch (Exception e) {
            log.error("Error archiving order to S3: {}", order.getOrderId(), e);
            throw new RuntimeException("Failed to archive order", e);
        }
    }
}
