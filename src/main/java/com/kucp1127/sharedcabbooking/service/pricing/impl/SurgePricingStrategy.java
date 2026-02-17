package com.kucp1127.sharedcabbooking.service.pricing.impl;

import com.kucp1127.sharedcabbooking.dto.request.RideRequest;
import com.kucp1127.sharedcabbooking.repository.BookingRepository;
import com.kucp1127.sharedcabbooking.repository.PricingConfigRepository;
import com.kucp1127.sharedcabbooking.service.pricing.PricingContext;
import com.kucp1127.sharedcabbooking.service.pricing.PricingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Surge pricing strategy based on demand.
 * Formula: SurgedFare = BaseFare * SurgeMultiplier
 *
 * Surge Multiplier:
 * - Low demand (< 50 active): 1.0x
 * - Medium demand (50-100): 1.2x
 * - High demand (100-200): 1.5x
 * - Very high demand (> 200): 2.0x (capped)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SurgePricingStrategy implements PricingStrategy {

    private final PricingConfigRepository pricingConfigRepository;
    private final BookingRepository bookingRepository;

    private static final BigDecimal MAX_SURGE = new BigDecimal("2.0");

    @Override
    public BigDecimal calculateFare(RideRequest request, BigDecimal currentFare, PricingContext context) {
        Double surgeMultiplier = calculateSurgeMultiplier(context);
        context.setSurgeMultiplier(surgeMultiplier);

        BigDecimal multiplier = BigDecimal.valueOf(surgeMultiplier);
        BigDecimal surgedFare = currentFare.multiply(multiplier);

        log.debug("Surge applied: {} * {} = {}", currentFare, surgeMultiplier, surgedFare);

        return surgedFare.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getStrategyName() {
        return "SURGE_PRICING";
    }

    @Override
    public int getPriority() {
        return 2; // After base fare
    }

    /**
     * Calculate surge multiplier based on current demand.
     */
    private Double calculateSurgeMultiplier(PricingContext context) {
        long activeBookings = context.getActiveBookingsCount();

        if (activeBookings == 0) {
            activeBookings = bookingRepository.countActiveBookings();
        }

        // Get thresholds from config
        BigDecimal lowThreshold = getConfigValue("SURGE", "LOW_DEMAND_THRESHOLD", new BigDecimal("50"));
        BigDecimal mediumThreshold = getConfigValue("SURGE", "MEDIUM_DEMAND_THRESHOLD", new BigDecimal("100"));
        BigDecimal highThreshold = getConfigValue("SURGE", "HIGH_DEMAND_THRESHOLD", new BigDecimal("200"));

        BigDecimal lowMultiplier = getConfigValue("SURGE", "LOW_SURGE_MULTIPLIER", new BigDecimal("1.2"));
        BigDecimal mediumMultiplier = getConfigValue("SURGE", "MEDIUM_SURGE_MULTIPLIER", new BigDecimal("1.5"));
        BigDecimal highMultiplier = getConfigValue("SURGE", "HIGH_SURGE_MULTIPLIER", new BigDecimal("2.0"));

        if (activeBookings < lowThreshold.intValue()) {
            return 1.0; // No surge
        } else if (activeBookings < mediumThreshold.intValue()) {
            return lowMultiplier.doubleValue();
        } else if (activeBookings < highThreshold.intValue()) {
            return mediumMultiplier.doubleValue();
        } else {
            return Math.min(highMultiplier.doubleValue(), MAX_SURGE.doubleValue());
        }
    }

    private BigDecimal getConfigValue(String type, String key, BigDecimal defaultValue) {
        return pricingConfigRepository.getNumericValue(type, key)
                .orElse(defaultValue);
    }
}
