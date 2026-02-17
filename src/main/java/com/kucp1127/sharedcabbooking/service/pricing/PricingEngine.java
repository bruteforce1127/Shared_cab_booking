package com.kucp1127.sharedcabbooking.service.pricing;

import com.kucp1127.sharedcabbooking.domain.entity.Location;
import com.kucp1127.sharedcabbooking.dto.request.RideRequest;
import com.kucp1127.sharedcabbooking.dto.response.FareEstimateResponse;
import com.kucp1127.sharedcabbooking.repository.BookingRepository;
import com.kucp1127.sharedcabbooking.repository.PricingConfigRepository;
import com.kucp1127.sharedcabbooking.util.DistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * Pricing engine that orchestrates all pricing strategies.
 * Implements Chain of Responsibility pattern for pricing modifiers.
 *
 * Pricing Formula:
 * FinalPrice = (BaseFare + DistanceCharge) * SurgeMultiplier * CabTypeMultiplier * (1 - SharingDiscount)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PricingEngine {

    private final List<PricingStrategy> pricingStrategies;
    private final DistanceCalculator distanceCalculator;
    private final BookingRepository bookingRepository;
    private final PricingConfigRepository pricingConfigRepository;

    /**
     * Calculate fare estimate for a ride request.
     */
    public FareEstimateResponse calculateFareEstimate(RideRequest request) {
        log.info("Calculating fare estimate for passenger: {}", request.getPassengerId());

        // Calculate distance
        double distanceKm = distanceCalculator.calculateDistance(
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDropoffLatitude(), request.getDropoffLongitude()
        );

        // Build pricing context
        PricingContext context = PricingContext.builder()
                .distanceKm(distanceKm)
                .requestTime(request.getRequestedPickupTime())
                .cabType(request.getPreferredCabType() != null ? request.getPreferredCabType() : "SEDAN")
                .estimatedCoPassengers(estimateCoPassengers(request))
                .activeBookingsCount(bookingRepository.countActiveBookings())
                .surgeMultiplier(1.0)
                .isAirportRide(true)
                .build();

        // Calculate fare using chain of strategies
        BigDecimal fare = calculateFareWithStrategies(request, context);

        // Build detailed response
        return buildFareEstimateResponse(request, context, fare, distanceKm);
    }

    /**
     * Calculate fare by applying all strategies in order.
     */
    private BigDecimal calculateFareWithStrategies(RideRequest request, PricingContext context) {
        BigDecimal currentFare = BigDecimal.ZERO;

        // Sort strategies by priority and apply in order
        List<PricingStrategy> sortedStrategies = pricingStrategies.stream()
                .sorted(Comparator.comparingInt(PricingStrategy::getPriority))
                .toList();

        for (PricingStrategy strategy : sortedStrategies) {
            log.debug("Applying pricing strategy: {}", strategy.getStrategyName());
            currentFare = strategy.calculateFare(request, currentFare, context);
        }

        return currentFare.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Estimate number of co-passengers for sharing discount calculation.
     */
    private Integer estimateCoPassengers(RideRequest request) {
        // Simple estimation based on time of day and location
        // In production, this would use historical data and ML models
        int hour = request.getRequestedPickupTime().getHour();

        // Peak hours = more likely to find co-passengers
        if ((hour >= 6 && hour <= 10) || (hour >= 17 && hour <= 21)) {
            return 2; // Estimated 2 co-passengers during peak
        } else if (hour >= 10 && hour <= 17) {
            return 1; // Estimated 1 co-passenger during day
        } else {
            return 0; // Off-peak, solo ride likely
        }
    }

    /**
     * Build detailed fare estimate response.
     */
    private FareEstimateResponse buildFareEstimateResponse(RideRequest request,
            PricingContext context, BigDecimal totalFare, double distanceKm) {

        // Calculate components for transparency
        BigDecimal perKmRate = pricingConfigRepository.getNumericValue("BASE_FARE", "PER_KM_RATE")
                .orElse(new BigDecimal("15.00"));
        BigDecimal bookingFee = pricingConfigRepository.getNumericValue("BASE_FARE", "BOOKING_FEE")
                .orElse(new BigDecimal("25.00"));

        BigDecimal distanceCharge = perKmRate.multiply(BigDecimal.valueOf(distanceKm))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal baseFare = bookingFee.add(distanceCharge);

        // Calculate surge charge
        BigDecimal surgeCharge = BigDecimal.ZERO;
        if (context.getSurgeMultiplier() > 1.0) {
            surgeCharge = baseFare.multiply(BigDecimal.valueOf(context.getSurgeMultiplier() - 1.0))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Calculate sharing discount
        BigDecimal estimatedDiscount = BigDecimal.ZERO;
        if (context.getEstimatedCoPassengers() > 0) {
            BigDecimal discountRate = new BigDecimal("0.05")
                    .multiply(BigDecimal.valueOf(context.getEstimatedCoPassengers()));
            estimatedDiscount = totalFare.multiply(discountRate)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        String message = generatePricingMessage(context);

        return FareEstimateResponse.builder()
                .baseFare(baseFare)
                .distanceCharge(distanceCharge)
                .bookingFee(bookingFee)
                .surgeCharge(surgeCharge)
                .estimatedSharingDiscount(estimatedDiscount)
                .estimatedTotalFare(totalFare)
                .surgeMultiplier(context.getSurgeMultiplier())
                .estimatedDistanceKm(distanceKm)
                .estimatedCoPassengers(context.getEstimatedCoPassengers())
                .message(message)
                .build();
    }

    /**
     * Generate user-friendly pricing message.
     */
    private String generatePricingMessage(PricingContext context) {
        StringBuilder message = new StringBuilder();

        if (context.getSurgeMultiplier() > 1.5) {
            message.append("High demand - surge pricing in effect. ");
        } else if (context.getSurgeMultiplier() > 1.0) {
            message.append("Moderate demand. ");
        }

        if (context.getEstimatedCoPassengers() > 0) {
            message.append(String.format("Share your ride and save up to %d%%!",
                    context.getEstimatedCoPassengers() * 5));
        } else {
            message.append("Book now for best rates.");
        }

        return message.toString();
    }

    /**
     * Get current surge multiplier (cached for performance).
     */
    @Cacheable(value = "surgeMultiplier", key = "'current'", unless = "#result == 1.0")
    public Double getCurrentSurgeMultiplier() {
        long activeBookings = bookingRepository.countActiveBookings();

        BigDecimal lowThreshold = pricingConfigRepository.getNumericValue("SURGE", "LOW_DEMAND_THRESHOLD")
                .orElse(new BigDecimal("50"));
        BigDecimal mediumThreshold = pricingConfigRepository.getNumericValue("SURGE", "MEDIUM_DEMAND_THRESHOLD")
                .orElse(new BigDecimal("100"));
        BigDecimal highThreshold = pricingConfigRepository.getNumericValue("SURGE", "HIGH_DEMAND_THRESHOLD")
                .orElse(new BigDecimal("200"));

        if (activeBookings < lowThreshold.intValue()) {
            return 1.0;
        } else if (activeBookings < mediumThreshold.intValue()) {
            return 1.2;
        } else if (activeBookings < highThreshold.intValue()) {
            return 1.5;
        } else {
            return 2.0;
        }
    }
}
