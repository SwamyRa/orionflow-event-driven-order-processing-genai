package com.orderprocessing.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.codepipeline.CodePipelineClient;
import software.amazon.awssdk.services.codepipeline.model.PutJobFailureResultRequest;
import software.amazon.awssdk.services.codepipeline.model.PutJobSuccessResultRequest;

import java.util.Map;

@Slf4j
public class OrderProcessorHandler implements RequestHandler<Map<String, Object>, String> {
    
    private final ObjectMapper objectMapper;
    private final CodePipelineClient codePipelineClient;
    
    public OrderProcessorHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.codePipelineClient = CodePipelineClient.builder().build();
    }
    
    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        String jobId = null;
        
        try {
            // Extract CodePipeline job ID
            Map<String, Object> codePipelineJob = (Map<String, Object>) input.get("CodePipeline.job");
            jobId = (String) codePipelineJob.get("id");
            
            log.info("Processing CodePipeline job: {}", jobId);
            
            // Notify success
            codePipelineClient.putJobSuccessResult(
                PutJobSuccessResultRequest.builder()
                    .jobId(jobId)
                    .build()
            );
            
            log.info("CodePipeline job completed successfully: {}", jobId);
            return "SUCCESS";
            
        } catch (Exception e) {
            log.error("Error processing CodePipeline job", e);
            
            if (jobId != null) {
                try {
                    codePipelineClient.putJobFailureResult(
                        PutJobFailureResultRequest.builder()
                            .jobId(jobId)
                            .failureDetails(builder -> builder.message(e.getMessage()))
                            .build()
                    );
                } catch (Exception ex) {
                    log.error("Failed to report job failure", ex);
                }
            }
            throw new RuntimeException(e);
        }
    }
}
