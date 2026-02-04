package com.orderprocessing.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orderprocessing.constants.OrderStatus;
import com.orderprocessing.model.*;
import com.orderprocessing.service.*;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main Lambda handler for AI-powered order processing with FinOps tracking.
 *
 * Processing flow:
 * 1. Validate order (save Bedrock costs on invalid orders)
 * 2. Call Bedrock AI for fraud detection
 * 3. Make approval decision based on AI score
 * 4. Calculate real-time processing costs
 * 5. Save to DynamoDB (hot storage)
 * 6. Archive to S3 (cold storage + compliance)
 * 7. Send SNS notification
 * 8. Publish CloudWatch metrics for FinOps
 *
 * Handler: com.orderprocessing.handler.OrderProcessorHandler::handleRequest
 */
@Slf4j
public class OrderProcessorHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private final ObjectMapper objectMapper;
    private final ValidationService validationService;
    private final BedrockService bedrockService;
    private final CostCalculatorService costCalculatorService;
    private final DynamoDBService dynamoDBService;
    private final S3Service s3Service;
    private final SNSService snsService;
    private final CloudWatchService cloudWatchService;

    private static final int LAMBDA_MEMORY_MB = 512;

    public OrderProcessorHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        // Initialize AWS clients
        BedrockRuntimeClient bedrockClient = BedrockRuntimeClient.builder().build();
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        S3Client s3Client = S3Client.builder().build();
        SnsClient snsClient = SnsClient.builder().build();
        CloudWatchClient cloudWatchClient = CloudWatchClient.builder().build();

        // Initialize services
        this.validationService = new ValidationService();
        this.bedrockService = new BedrockService(bedrockClient);
        this.costCalculatorService = new CostCalculatorService();
        this.dynamoDBService = new DynamoDBService(dynamoDbClient, System.getenv("DYNAMODB_TABLE_NAME"));
        this.s3Service = new S3Service(s3Client, System.getenv("S3_BUCKET_NAME"));
        this.snsService = new SNSService(snsClient, System.getenv("SNS_TOPIC_ARN"));
        this.cloudWatchService = new CloudWatchService(cloudWatchClient);
    }

    /**
     * Main entry point for Lambda function.
     * Processes order through complete fraud detection and storage pipeline.
     *
     * @param input API Gateway request with order JSON in body
     * @param context Lambda execution context
     * @return API Gateway response with order status and cost metrics
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Processing order request");

            // Parse order from request body
            Order order = objectMapper.readValue(input.getBody(), Order.class);
            order.setTimestamp(Instant.now());

            // Step 1: Validate order
            List<String> validationErrors = validationService.validateOrder(order);
            if (!validationErrors.isEmpty()) {
                return handleValidationError(order, validationErrors, startTime);
            }

            // Step 2: Call Bedrock for fraud detection
            BedrockAnalysisResult bedrockResult = bedrockService.analyzeOrder(order);

            // Step 3: Make decision based on Bedrock score
            order.setAiScore(bedrockResult.getScore());
            order.setProcessedAt(Instant.now());

            if ("APPROVED".equals(bedrockResult.getDecision())) {
                order.setStatus(OrderStatus.APPROVED);
            } else if ("REJECTED".equals(bedrockResult.getDecision())) {
                order.setStatus(OrderStatus.REJECTED);
                order.setRejectionReasons(bedrockResult.getFraudIndicators());
            } else {
                order.setStatus(OrderStatus.PENDING_REVIEW);
                order.setRejectionReasons(bedrockResult.getFraudIndicators());
            }

            // Step 4: Calculate costs
            long duration = System.currentTimeMillis() - startTime;
            CostMetrics costMetrics = costCalculatorService.calculateCosts(
                    duration,
                    LAMBDA_MEMORY_MB,
                    bedrockResult.getTokensUsed()
            );
            order.setCostMetrics(costMetrics);

            // Step 5: Save to DynamoDB
            dynamoDBService.saveOrder(order);

            // Step 6: Archive to S3
            s3Service.archiveOrder(order);

            // Step 7: Send SNS notification
            snsService.sendNotification(order);

            // Step 8: Publish CloudWatch metrics
            cloudWatchService.publishMetrics(order.getOrderId(), order.getStatus().name(), costMetrics);

            // Build response
            OrderResponse response = OrderResponse.builder()
                    .orderId(order.getOrderId())
                    .status(order.getStatus())
                    .message(buildMessage(order))
                    .aiScore(order.getAiScore())
                    .rejectionReasons(order.getRejectionReasons())
                    .costMetrics(costMetrics)
                    .timestamp(order.getProcessedAt().toString())
                    .build();

            log.info("Order processed successfully: {} - Status: {}", order.getOrderId(), order.getStatus());

            return buildApiResponse(200, response);

        } catch (Exception e) {
            log.error("Error processing order", e);
            return buildErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Handles validation errors without calling Bedrock (cost optimization).
     */
    private APIGatewayProxyResponseEvent handleValidationError(Order order, List<String> errors, long startTime) {
        try {
            order.setStatus(OrderStatus.VALIDATION_ERROR);
            order.setRejectionReasons(errors);
            order.setProcessedAt(Instant.now());

            // Calculate minimal cost (no Bedrock call)
            long duration = System.currentTimeMillis() - startTime;
            CostMetrics costMetrics = costCalculatorService.calculateCosts(duration, LAMBDA_MEMORY_MB, 0);
            order.setCostMetrics(costMetrics);

            OrderResponse response = OrderResponse.builder()
                    .orderId(order.getOrderId())
                    .status(OrderStatus.VALIDATION_ERROR)
                    .message("Validation failed")
                    .rejectionReasons(errors)
                    .costMetrics(costMetrics)
                    .timestamp(Instant.now().toString())
                    .build();

            return buildApiResponse(400, response);

        } catch (Exception e) {
            log.error("Error handling validation error", e);
            return buildErrorResponse(500, "Internal server error");
        }
    }

    /**
     * Builds user-friendly message based on order status.
     */
    private String buildMessage(Order order) {
        switch (order.getStatus()) {
            case APPROVED:
                return "Order processed successfully";
            case REJECTED:
                return "Order rejected due to fraud indicators";
            case PENDING_REVIEW:
                return "Order requires manual review";
            default:
                return "Order processed";
        }
    }

    /**
     * Builds API Gateway response with CORS headers.
     */
    private APIGatewayProxyResponseEvent buildApiResponse(int statusCode, Object body) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Access-Control-Allow-Origin", "*");

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withHeaders(headers)
                    .withBody(objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            log.error("Error building API response", e);
            return buildErrorResponse(500, "Error building response");
        }
    }

    /**
     * Builds error response with fallback for JSON serialization failures.
     */
    private APIGatewayProxyResponseEvent buildErrorResponse(int statusCode, String message) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("error", message);

        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withHeaders(headers)
                    .withBody(objectMapper.writeValueAsString(errorBody));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withHeaders(headers)
                    .withBody("{\"error\":\"" + message + "\"}");
        }
    }
}
