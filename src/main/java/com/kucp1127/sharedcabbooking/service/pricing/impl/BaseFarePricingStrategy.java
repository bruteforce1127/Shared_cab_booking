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
 * Base fare calculation strategy.
 * Formula: BaseFare = BookingFee + (DistanceKm * PerKmRate)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BaseFarePricingStrategy implements PricingStrategy {

    private final PricingConfigRepository pricingConfigRepository;

    private static final BigDecimal DEFAULT_PER_KM_RATE = new BigDecimal("15.00");
    private static final BigDecimal DEFAULT_BOOKING_FEE = new BigDecimal("25.00");
    private static final BigDecimal DEFAULT_MINIMUM_FARE = new BigDecimal("100.00");

    @Override
    public BigDecimal calculateFare(RideRequest request, BigDecimal currentFare, PricingContext context) {
        BigDecimal perKmRate = getConfigValue("BASE_FARE", "PER_KM_RATE", DEFAULT_PER_KM_RATE);
        BigDecimal bookingFee = getConfigValue("BASE_FARE", "BOOKING_FEE", DEFAULT_BOOKING_FEE);
        BigDecimal minimumFare = getConfigValue("BASE_FARE", "MINIMUM_FARE", DEFAULT_MINIMUM_FARE);

        BigDecimal distanceCharge = perKmRate.multiply(BigDecimal.valueOf(context.getDistanceKm()));
        BigDecimal baseFare = bookingFee.add(distanceCharge);

        // Apply minimum fare
        if (baseFare.compareTo(minimumFare) < 0) {
            baseFare = minimumFare;
        }

        log.debug("Base fare calculated: {} (distance: {} km, per km: {})",
                baseFare, context.getDistanceKm(), perKmRate);

        return baseFare.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getStrategyName() {
        return "BASE_FARE";
    }

    @Override
    public int getPriority() {
        return 1; // First in chain
    }

    private BigDecimal getConfigValue(String type, String key, BigDecimal defaultValue) {
        return pricingConfigRepository.getNumericValue(type, key)
                .orElse(defaultValue);
    }
}
