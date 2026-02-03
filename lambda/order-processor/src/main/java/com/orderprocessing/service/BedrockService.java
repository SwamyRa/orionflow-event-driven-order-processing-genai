package com.orderprocessing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderprocessing.model.BedrockAnalysisResult;
import com.orderprocessing.model.Order;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for AI-powered fraud detection using Amazon Bedrock Converse API.
 * 
 * Uses Converse API for model-agnostic implementation.
 * Change BEDROCK_MODEL_ID env var to switch between Claude, Llama, Titan, Nova without code changes.
 * 
 * Supported models:
 * - anthropic.claude-3-sonnet-20240229-v1:0
 * - anthropic.claude-3-haiku-20240307-v1:0
 * - meta.llama3-3-70b-instruct-v1:0
 * - amazon.titan-text-premier-v1:0
 * - amazon.nova-pro-v1:0
 * 
 * Analyzes orders based on:
 * - Email patterns (20% weight)
 * - Order value (20% weight)
 * - Quantity analysis (15% weight)
 * - Shipping address (20% weight)
 * - Product type (10% weight)
 * - Customer history (10% weight)
 * - Timing patterns (5% weight)
 */
@Slf4j
public class BedrockService {

    private final BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper;
    private final String modelId;

    public BedrockService(BedrockRuntimeClient bedrockClient) {
        this.bedrockClient = bedrockClient;
        this.objectMapper = new ObjectMapper();
        // Read from environment variable set by Terraform
        this.modelId = System.getenv("BEDROCK_MODEL_ID");
        log.info("BedrockService initialized with model: {}", modelId);
    }

    /**
     * Analyzes order for fraud indicators using Bedrock AI via Converse API.
     * 
     * @param order Order to analyze
     * @return BedrockAnalysisResult with fraud score, decision, and indicators
     * @throws RuntimeException if Bedrock call fails
     */
    public BedrockAnalysisResult analyzeOrder(Order order) {
        try {
            String prompt = buildFraudDetectionPrompt(order);

            log.info("Calling Bedrock Converse API for order: {} with model: {}", order.getOrderId(), modelId);

            // Build Converse API request
            Message userMessage = Message.builder()
                    .role(ConversationRole.USER)
                    .content(ContentBlock.fromText(prompt))
                    .build();

            ConverseRequest request = ConverseRequest.builder()
                    .modelId(modelId)
                    .messages(userMessage)
                    .inferenceConfig(InferenceConfiguration.builder()
                            .maxTokens(1000)
                            .temperature(0.1f)
                            .build())
                    .build();

            ConverseResponse response = bedrockClient.converse(request);

            return parseConverseResponse(response);

        } catch (Exception e) {
            log.error("Error calling Bedrock for order: {}", order.getOrderId(), e);
            throw new RuntimeException("Bedrock analysis failed: " + e.getMessage(), e);
        }
    }

    /**
     * Builds detailed fraud detection prompt with scoring rules.
     * Includes all order details and weighted fraud indicators.
     */
    private String buildFraudDetectionPrompt(Order order) {
        String itemsFormatted = order.getItems().stream()
                .map(item -> String.format("  - %s (Qty: %d, Price: $%.2f)",
                        item.getName(), item.getQuantity(), item.getPrice()))
                .collect(Collectors.joining("\n"));

        return String.format("""
            You are an expert fraud detection system for an e-commerce platform.
            Your task is to analyze the following order and assign a fraud risk score from 0 to 10.
            
            SCORING RULES:
            - Score 0-3: HIGH RISK (Reject order)
            - Score 4-6: MEDIUM RISK (Manual review required)
            - Score 7-10: LOW RISK (Approve order)
            
            FRAUD INDICATORS TO CHECK:
            
            1. EMAIL ANALYSIS (Weight: 20%%)
               - Disposable email domains (tempmail, guerrillamail, 10minutemail): -3 points
               - Free email with suspicious patterns: -1 point
               - Corporate/business email: +1 point
            
            2. ORDER VALUE ANALYSIS (Weight: 20%%)
               - Order < $50: Low risk (+1 point)
               - Order > $2000 from new customer: High risk (-2 points)
               - Order > $5000: Very high risk (-3 points)
            
            3. QUANTITY ANALYSIS (Weight: 15%%)
               - > 10 items from new customer: High risk (-2 points)
               - > 20 items: Very high risk (-3 points)
            
            4. SHIPPING ADDRESS ANALYSIS (Weight: 20%%)
               - Complete, valid address: +1 point
               - Incomplete address: -2 points
               - Invalid zip code: -1 point
            
            5. PRODUCT TYPE ANALYSIS (Weight: 10%%)
               - High-risk products (gift cards, electronics in bulk): -2 points
            
            6. CUSTOMER HISTORY (Weight: 10%%)
               - New customer: -1 point
               - VIP customer (> 20 orders): +2 points
            
            7. TIMING ANALYSIS (Weight: 5%%)
               - Order placed late night (12 AM - 6 AM): -1 point
            
            ORDER DETAILS:
            
            Order ID: %s
            Customer ID: %s
            Customer Email: %s
            Customer Type: %s
            Order History: %d previous orders
            
            Items:
            %s
            
            Total Amount: $%.2f
            
            Shipping Address:
            %s
            %s, %s %s
            %s
            
            Payment Method: %s
            
            RESPONSE FORMAT (JSON):
            {
              "score": <number 0-10>,
              "risk_level": "<LOW|MEDIUM|HIGH>",
              "decision": "<APPROVED|PENDING_REVIEW|REJECTED>",
              "confidence": <number 0-100>,
              "fraud_indicators": ["List of specific fraud indicators found"],
              "reasoning": "Brief explanation of the decision",
              "recommendations": ["Any recommendations"]
            }
            
            Analyze the order and provide your assessment in JSON format only.
            """,
                order.getOrderId(),
                order.getCustomerId(),
                order.getCustomerEmail(),
                order.getCustomerType() != null ? order.getCustomerType() : "REGULAR",
                order.getOrderHistory() != null ? order.getOrderHistory() : 0,
                itemsFormatted,
                order.getTotalAmount(),
                order.getShippingAddress().getStreet(),
                order.getShippingAddress().getCity(),
                order.getShippingAddress().getState() != null ? order.getShippingAddress().getState() : "",
                order.getShippingAddress().getZipCode() != null ? order.getShippingAddress().getZipCode() : "",
                order.getShippingAddress().getCountry(),
                order.getPaymentMethod()
        );
    }

    /**
     * Parses Converse API response and extracts fraud analysis JSON.
     * Handles both plain JSON and markdown-wrapped responses.
     */
    private BedrockAnalysisResult parseConverseResponse(ConverseResponse response) throws Exception {
        // Extract text from response
        String content = response.output().message().content().get(0).text();
        
        log.debug("Bedrock response: {}", content);

        // Extract JSON from response (handle markdown code blocks)
        String jsonContent = content;
        if (content.contains("```json")) {
            jsonContent = content.substring(content.indexOf("```json") + 7, content.lastIndexOf("```")).trim();
        } else if (content.contains("```")) {
            jsonContent = content.substring(content.indexOf("```") + 3, content.lastIndexOf("```")).trim();
        } else if (content.contains("{")) {
            int start = content.indexOf("{");
            int end = content.lastIndexOf("}") + 1;
            jsonContent = content.substring(start, end);
        }

        JsonNode analysis = objectMapper.readTree(jsonContent);

        BedrockAnalysisResult result = new BedrockAnalysisResult();
        result.setScore(analysis.path("score").asDouble());
        result.setRiskLevel(analysis.path("risk_level").asText());
        result.setDecision(analysis.path("decision").asText());
        result.setConfidence(analysis.path("confidence").asInt());
        result.setReasoning(analysis.path("reasoning").asText());

        List<String> indicators = new ArrayList<>();
        analysis.path("fraud_indicators").forEach(node -> indicators.add(node.asText()));
        result.setFraudIndicators(indicators);

        List<String> recommendations = new ArrayList<>();
        analysis.path("recommendations").forEach(node -> recommendations.add(node.asText()));
        result.setRecommendations(recommendations);

        // Get token usage from response metadata
        int inputTokens = response.usage().inputTokens();
        int outputTokens = response.usage().outputTokens();
        result.setTokensUsed(inputTokens + outputTokens);

        log.info("Bedrock analysis complete - Score: {}, Decision: {}, Tokens: {} (in: {}, out: {})",
                result.getScore(), result.getDecision(), result.getTokensUsed(), inputTokens, outputTokens);

        return result;
    }
}
