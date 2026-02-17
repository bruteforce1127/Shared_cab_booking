package com.kucp1127.sharedcabbooking.service.pricing;

import com.kucp1127.sharedcabbooking.dto.request.RideRequest;

import java.math.BigDecimal;

/**
 * Strategy interface for pricing algorithms.
 * Implements Strategy Pattern for flexible pricing logic.
 */
public interface PricingStrategy {

    /**
     * Calculate fare component for a ride request.
     * @param request The ride request
     * @param currentFare Current fare (for chained calculations)
     * @param context Pricing context with additional data
     * @return Calculated fare component
     */
    BigDecimal calculateFare(RideRequest request, BigDecimal currentFare, PricingContext context);

    /**
     * Get the name of this pricing strategy.
     */
    String getStrategyName();

    /**
     * Get priority for applying this strategy (lower = earlier).
     */
    int getPriority();
}
