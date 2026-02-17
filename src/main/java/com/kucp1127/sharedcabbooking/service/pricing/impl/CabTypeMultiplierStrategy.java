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
 * Cab type multiplier strategy.
 * Different cab types have different base rates.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CabTypeMultiplierStrategy implements PricingStrategy {

    private final PricingConfigRepository pricingConfigRepository;

    @Override
    public BigDecimal calculateFare(RideRequest request, BigDecimal currentFare, PricingContext context) {
        String cabType = context.getCabType();
        if (cabType == null || cabType.isEmpty()) {
            cabType = "SEDAN";
        }

        BigDecimal multiplier = getMultiplierForCabType(cabType);
        BigDecimal adjustedFare = currentFare.multiply(multiplier);

        log.debug("Cab type multiplier applied: {} * {} = {}",
                currentFare, multiplier, adjustedFare);

        return adjustedFare.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getStrategyName() {
        return "CAB_TYPE_MULTIPLIER";
    }

    @Override
    public int getPriority() {
        return 3; // After surge
    }

    private BigDecimal getMultiplierForCabType(String cabType) {
        String configKey = cabType.toUpperCase() + "_MULTIPLIER";
        return pricingConfigRepository.getNumericValue("CAB_TYPE", configKey)
                .orElse(BigDecimal.ONE);
    }
}
