package com.kucp1127.sharedcabbooking.service.pricing.impl;

import com.kucp1127.sharedcabbooking.dto.request.RideRequest;
import com.kucp1127.sharedcabbooking.repository.PricingConfigRepository;
import com.kucp1127.sharedcabbooking.service.pricing.PricingContext;
import com.kucp1127.sharedcabbooking.service.pricing.PricingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Shared ride discount strategy.
 * Formula: DiscountedFare = Fare * (1 - (CoPassengers * DiscountPerPerson))
 *
 * Discount: 5% per co-passenger, max 25%
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SharedRideDiscountStrategy implements PricingStrategy {

    private final PricingConfigRepository pricingConfigRepository;

    private static final BigDecimal DEFAULT_PER_COPASSENGER_DISCOUNT = new BigDecimal("0.05");
    private static final BigDecimal DEFAULT_MAX_DISCOUNT = new BigDecimal("0.25");

    @Override
    public BigDecimal calculateFare(RideRequest request, BigDecimal currentFare, PricingContext context) {
        Integer coPassengers = context.getEstimatedCoPassengers();

        if (coPassengers == null || coPassengers <= 0) {
            log.debug("No co-passengers, no sharing discount applied");
            return currentFare;
        }

        BigDecimal perPersonDiscount = getConfigValue("DISCOUNT", "PER_COPASSENGER_DISCOUNT",
                DEFAULT_PER_COPASSENGER_DISCOUNT);
        BigDecimal maxDiscount = getConfigValue("DISCOUNT", "MAX_SHARING_DISCOUNT",
                DEFAULT_MAX_DISCOUNT);

        // Calculate discount
        BigDecimal totalDiscountRate = perPersonDiscount.multiply(BigDecimal.valueOf(coPassengers));

        // Cap at maximum
        if (totalDiscountRate.compareTo(maxDiscount) > 0) {
            totalDiscountRate = maxDiscount;
        }

        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(totalDiscountRate);
        BigDecimal discountedFare = currentFare.multiply(discountMultiplier);

        log.debug("Sharing discount applied: {} co-passengers, {}% off, {} -> {}",
                coPassengers, totalDiscountRate.multiply(BigDecimal.valueOf(100)),
                currentFare, discountedFare);

        return discountedFare.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getStrategyName() {
        return "SHARED_RIDE_DISCOUNT";
    }

    @Override
    public int getPriority() {
        return 10; // Last in chain (applied to final fare)
    }

    private BigDecimal getConfigValue(String type, String key, BigDecimal defaultValue) {
        return pricingConfigRepository.getNumericValue(type, key)
                .orElse(defaultValue);
    }
}
