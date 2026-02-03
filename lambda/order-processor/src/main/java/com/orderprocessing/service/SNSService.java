package com.orderprocessing.service;

import com.orderprocessing.model.Order;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Service for sending order notifications via SNS.
 * 
 * Sends email notifications with:
 * - Order details (ID, customer, amount)
 * - Fraud analysis results
 * - Processing costs
 * - Rejection reasons (if applicable)
 * 
 * Note: Notification failures don't fail the order processing.
 */
@Slf4j
public class SNSService {
    
    private final SnsClient snsClient;
    private final String topicArn;
    
    public SNSService(SnsClient snsClient, String topicArn) {
        this.snsClient = snsClient;
        this.topicArn = topicArn;
    }
    
    /**
     * Sends SNS notification for order processing result.
     * Swallows exceptions to prevent notification failures from failing orders.
     * 
     * @param order Order to notify about
     */
    public void sendNotification(Order order) {
        try {
            String subject = String.format("Order %s - %s", order.getOrderId(), order.getStatus());
            String message = buildMessage(order);
            
            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .subject(subject)
                    .message(message)
                    .build();
            
            snsClient.publish(request);
            
            log.info("SNS notification sent for order: {}", order.getOrderId());
            
        } catch (Exception e) {
            log.error("Error sending SNS notification for order: {}", order.getOrderId(), e);
            // Don't throw exception - notification failure shouldn't fail the order
        }
    }
    
    /**
     * Builds human-readable notification message with order details.
     */
    private String buildMessage(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("Order ID: ").append(order.getOrderId()).append("\n");
        sb.append("Customer: ").append(order.getCustomerEmail()).append("\n");
        sb.append("Status: ").append(order.getStatus()).append("\n");
        sb.append("Amount: $").append(String.format("%.2f", order.getTotalAmount())).append("\n");
        
        if (order.getAiScore() != null) {
            sb.append("AI Score: ").append(String.format("%.1f", order.getAiScore())).append("/10\n");
        }
        
        if (order.getRejectionReasons() != null && !order.getRejectionReasons().isEmpty()) {
            sb.append("\nRejection Reasons:\n");
            order.getRejectionReasons().forEach(reason -> sb.append("- ").append(reason).append("\n"));
        }
        
        if (order.getCostMetrics() != null) {
            sb.append("\nProcessing Cost: $").append(String.format("%.5f", order.getCostMetrics().getTotalProcessingCost()));
        }
        
        return sb.toString();
    }
}
