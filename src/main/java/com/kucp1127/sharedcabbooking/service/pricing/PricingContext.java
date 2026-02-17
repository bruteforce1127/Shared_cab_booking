package com.kucp1127.sharedcabbooking.service.pricing;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Context object for pricing calculations.
 * Contains all data needed by pricing strategies.
 */
@Data
@Builder
public class PricingContext {

    private Double distanceKm;
    private LocalDateTime requestTime;
    private String cabType;
    private Integer estimatedCoPassengers;
    private Long activeBookingsCount;
    private Double surgeMultiplier;
    private Boolean isAirportRide;

    public static PricingContext createDefault() {
        return PricingContext.builder()
                .distanceKm(0.0)
                .requestTime(LocalDateTime.now())
                .cabType("SEDAN")
                .estimatedCoPassengers(0)
                .activeBookingsCount(0L)
                .surgeMultiplier(1.0)
                .isAirportRide(true)
                .build();
    }
}
