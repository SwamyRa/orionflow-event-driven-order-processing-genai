package com.orderprocessing.service;

import com.orderprocessing.model.CostMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CostCalculatorServiceTest {
    
    private CostCalculatorService costCalculatorService;
    
    @BeforeEach
    void setUp() {
        costCalculatorService = new CostCalculatorService();
    }
    
    @Test
    void testCalculateCosts_WithBedrockTokens() {
        // Given
        long durationMs = 1000;
        int memoryMB = 512;
        int bedrockTokens = 1500;
        
        // When
        CostMetrics metrics = costCalculatorService.calculateCosts(durationMs, memoryMB, bedrockTokens);
        
        // Then
        assertNotNull(metrics);
        assertEquals(1500, metrics.getBedrockTokensUsed());
        assertEquals(0.0045, metrics.getBedrockCost(), 0.0001);
        assertTrue(metrics.getLambdaCost() > 0);
        assertTrue(metrics.getTotalProcessingCost() > 0);
    }
    
    @Test
    void testCalculateCosts_WithoutBedrockTokens() {
        // Given
        long durationMs = 500;
        int memoryMB = 512;
        int bedrockTokens = 0;
        
        // When
        CostMetrics metrics = costCalculatorService.calculateCosts(durationMs, memoryMB, bedrockTokens);
        
        // Then
        assertNotNull(metrics);
        assertEquals(0, metrics.getBedrockTokensUsed());
        assertEquals(0.0, metrics.getBedrockCost());
        assertTrue(metrics.getTotalProcessingCost() > 0);
    }
}
