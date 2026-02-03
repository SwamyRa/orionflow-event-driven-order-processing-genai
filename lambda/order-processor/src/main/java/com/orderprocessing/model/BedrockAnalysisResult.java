package com.orderprocessing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BedrockAnalysisResult {
    
    @JsonProperty("score")
    private Double score;
    
    @JsonProperty("risk_level")
    private String riskLevel;
    
    @JsonProperty("decision")
    private String decision;
    
    @JsonProperty("confidence")
    private Integer confidence;
    
    @JsonProperty("fraud_indicators")
    private List<String> fraudIndicators;
    
    @JsonProperty("reasoning")
    private String reasoning;
    
    @JsonProperty("recommendations")
    private List<String> recommendations;
    
    private Integer tokensUsed;
}
