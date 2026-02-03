package com.orderprocessing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orderprocessing.constants.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("status")
    private OrderStatus status;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("aiScore")
    private Double aiScore;
    
    @JsonProperty("rejectionReasons")
    private List<String> rejectionReasons;
    
    @JsonProperty("costMetrics")
    private CostMetrics costMetrics;
    
    @JsonProperty("timestamp")
    private String timestamp;
}
