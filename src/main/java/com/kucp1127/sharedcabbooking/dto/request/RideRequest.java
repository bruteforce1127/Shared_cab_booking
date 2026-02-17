package com.kucp1127.sharedcabbooking.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideRequest {

    @NotNull(message = "Passenger ID is required")
    private Long passengerId;

    @NotNull(message = "Pickup latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private Double pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private Double pickupLongitude;

    private String pickupAddress;

    @NotNull(message = "Dropoff latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private Double dropoffLatitude;

    @NotNull(message = "Dropoff longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private Double dropoffLongitude;

    private String dropoffAddress;

    @NotNull(message = "Requested pickup time is required")
    @Future(message = "Pickup time must be in the future")
    private LocalDateTime requestedPickupTime;

    @Min(value = 1, message = "At least 1 passenger required")
    @Max(value = 6, message = "Maximum 6 passengers allowed")
    @Builder.Default
    private Integer passengerCount = 1;

    @Min(value = 0, message = "Luggage weight cannot be negative")
    @Max(value = 200, message = "Maximum 200 kg luggage allowed")
    @Builder.Default
    private Double luggageWeightKg = 0.0;

    @Min(value = 0, message = "Luggage count cannot be negative")
    @Max(value = 10, message = "Maximum 10 luggage pieces allowed")
    @Builder.Default
    private Integer luggageCount = 0;

    @DecimalMin(value = "0.0", message = "Detour tolerance must be >= 0")
    @DecimalMax(value = "0.5", message = "Detour tolerance must be <= 50%")
    @Builder.Default
    private Double maxDetourTolerance = 0.20;

    private String preferredCabType;

    private String specialRequirements;
}
