package com.kucp1127.sharedcabbooking.dto.response;

import lombok.*;

import java.math.BigDecimal;

/**
 * Response DTO for fare estimation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareEstimateResponse {

    private BigDecimal baseFare;
    private BigDecimal distanceCharge;
    private BigDecimal bookingFee;
    private BigDecimal surgeCharge;
    private BigDecimal estimatedSharingDiscount;
    private BigDecimal estimatedTotalFare;
    private Double surgeMultiplier;
    private Double estimatedDistanceKm;
    private Integer estimatedCoPassengers;
    private String message;
}
