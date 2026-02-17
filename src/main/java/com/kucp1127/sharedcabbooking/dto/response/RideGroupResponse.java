package com.kucp1127.sharedcabbooking.dto.response;

import com.kucp1127.sharedcabbooking.domain.enums.RideGroupStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for ride group information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideGroupResponse {

    private Long id;
    private RideGroupStatus status;
    private RideResponse.CabInfoDto cabInfo;
    private RideResponse.LocationDto airportLocation;
    private LocalDateTime scheduledDepartureTime;
    private LocalDateTime estimatedArrivalTime;
    private Integer totalPassengers;
    private Double totalLuggageWeightKg;
    private Double totalDistanceKm;
    private Double detourPercentage;
    private List<BookingSummary> bookings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingSummary {
        private Long bookingId;
        private Long passengerId;
        private String passengerName;
        private Integer pickupSequence;
        private RideResponse.LocationDto pickupLocation;
        private LocalDateTime estimatedPickupTime;
    }
}
