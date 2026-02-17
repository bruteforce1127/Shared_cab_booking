package com.kucp1127.sharedcabbooking.dto.response;

import com.kucp1127.sharedcabbooking.domain.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for ride/booking information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideResponse {

    private Long bookingId;
    private Long rideGroupId;
    private Long passengerId;

    private BookingStatus status;

    // Location details
    private LocationDto pickupLocation;
    private LocationDto dropoffLocation;

    // Timing
    private LocalDateTime requestedPickupTime;
    private LocalDateTime estimatedPickupTime;
    private LocalDateTime estimatedArrivalTime;

    // Ride details
    private Integer passengerCount;
    private Double luggageWeightKg;
    private Integer luggageCount;
    private Integer pickupSequence;

    // Pricing
    private BigDecimal estimatedFare;
    private BigDecimal sharingDiscount;
    private Double surgeMultiplier;

    // Co-passengers info (for shared rides)
    private Integer totalCoPassengers;
    private Double estimatedDetourPercentage;

    // Cab info (when assigned)
    private CabInfoDto cabInfo;

    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationDto {
        private Double latitude;
        private Double longitude;
        private String address;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CabInfoDto {
        private String licensePlate;
        private String driverName;
        private String driverPhone;
        private String cabType;
        private Double driverRating;
    }
}
