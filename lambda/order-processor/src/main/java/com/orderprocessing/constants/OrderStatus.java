package com.orderprocessing.constants;

/**
 * Order processing status enum.
 * 
 * APPROVED: Order passed fraud checks (AI score 7-10)
 * REJECTED: Order failed fraud checks (AI score 0-3)
 * PENDING_REVIEW: Order requires manual review (AI score 4-6)
 * VALIDATION_ERROR: Order failed input validation (no AI analysis)
 */
public enum OrderStatus {
    APPROVED,
    REJECTED,
    PENDING_REVIEW,
    VALIDATION_ERROR
}
