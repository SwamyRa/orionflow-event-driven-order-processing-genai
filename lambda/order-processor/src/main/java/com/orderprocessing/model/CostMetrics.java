package com.orderprocessing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostMetrics {
    
    @JsonProperty("bedrockTokensUsed")
    private Integer bedrockTokensUsed;
    
    @JsonProperty("bedrockCost")
    private Double bedrockCost;
    
    @JsonProperty("lambdaDurationMs")
    private Long lambdaDurationMs;
    
    @JsonProperty("lambdaCost")
    private Double lambdaCost;
    
    @JsonProperty("dynamodbWriteUnits")
    private Integer dynamodbWriteUnits;
    
    @JsonProperty("dynamodbCost")
    private Double dynamodbCost;
    
    @JsonProperty("s3PutRequests")
    private Integer s3PutRequests;
    
    @JsonProperty("s3Cost")
    private Double s3Cost;
    
    @JsonProperty("snsNotifications")
    private Integer snsNotifications;
    
    @JsonProperty("snsCost")
    private Double snsCost;
    
    @JsonProperty("apiGatewayCalls")
    private Integer apiGatewayCalls;
    
    @JsonProperty("apiGatewayCost")
    private Double apiGatewayCost;
    
    @JsonProperty("totalProcessingCost")
    private Double totalProcessingCost;
    
    public Double calculateTotal() {
        return (bedrockCost != null ? bedrockCost : 0.0) +
               (lambdaCost != null ? lambdaCost : 0.0) +
               (dynamodbCost != null ? dynamodbCost : 0.0) +
               (s3Cost != null ? s3Cost : 0.0) +
               (snsCost != null ? snsCost : 0.0) +
               (apiGatewayCost != null ? apiGatewayCost : 0.0);
    }
}
