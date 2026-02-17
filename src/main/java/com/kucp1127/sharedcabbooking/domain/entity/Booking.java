package com.kucp1127.sharedcabbooking.domain.entity;

import com.kucp1127.sharedcabbooking.domain.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_booking_status", columnList = "status"),
    @Index(name = "idx_booking_passenger_status", columnList = "passenger_id, status"),
    @Index(name = "idx_booking_pickup_time", columnList = "requested_pickup_time"),
    @Index(name = "idx_booking_ride_group", columnList = "ride_group_id"),
    @Index(name = "idx_booking_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_group_id")
    private RideGroup rideGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    /**
     * Pickup location for this passenger.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude", column = @Column(name = "pickup_latitude")),
        @AttributeOverride(name = "longitude", column = @Column(name = "pickup_longitude")),
        @AttributeOverride(name = "address", column = @Column(name = "pickup_address"))
    })
    private Location pickupLocation;

    /**
     * Drop-off location (typically airport).
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude", column = @Column(name = "dropoff_latitude")),
        @AttributeOverride(name = "longitude", column = @Column(name = "dropoff_longitude")),
        @AttributeOverride(name = "address", column = @Column(name = "dropoff_address"))
    })
    private Location dropoffLocation;

    /**
     * Requested pickup time by passenger.
     */
    @Column(name = "requested_pickup_time", nullable = false)
    private LocalDateTime requestedPickupTime;

    /**
     * Estimated pickup time after route optimization.
     */
    @Column(name = "estimated_pickup_time")
    private LocalDateTime estimatedPickupTime;

    /**
     * Actual pickup time.
     */
    @Column(name = "actual_pickup_time")
    private LocalDateTime actualPickupTime;

    /**
     * Number of passengers in this booking.
     */
    @Column(name = "passenger_count", nullable = false)
    @Builder.Default
    private Integer passengerCount = 1;

    /**
     * Total luggage weight in kg.
     */
    @Column(name = "luggage_weight_kg", nullable = false)
    @Builder.Default
    private Double luggageWeightKg = 0.0;

    /**
     * Number of luggage pieces.
     */
    @Column(name = "luggage_count", nullable = false)
    @Builder.Default
    private Integer luggageCount = 0;

    /**
     * Passenger's maximum acceptable detour percentage.
     */
    @Column(name = "max_detour_tolerance")
    @Builder.Default
    private Double maxDetourTolerance = 0.20;

    /**
     * Direct distance from pickup to dropoff in km.
     */
    @Column(name = "direct_distance_km")
    private Double directDistanceKm;

    /**
     * Actual distance traveled (may include detour).
     */
    @Column(name = "actual_distance_km")
    private Double actualDistanceKm;

    /**
     * Base fare before adjustments.
     */
    @Column(name = "base_fare", precision = 10, scale = 2)
    private BigDecimal baseFare;

    /**
     * Final fare after all adjustments (surge, discount, etc.).
     */
    @Column(name = "final_fare", precision = 10, scale = 2)
    private BigDecimal finalFare;

    /**
     * Sharing discount applied.
     */
    @Column(name = "sharing_discount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal sharingDiscount = BigDecimal.ZERO;

    /**
     * Surge multiplier at time of booking.
     */
    @Column(name = "surge_multiplier")
    @Builder.Default
    private Double surgeMultiplier = 1.0;

    /**
     * Special requirements or notes.
     */
    @Column(name = "special_requirements", columnDefinition = "TEXT")
    private String specialRequirements;

    /**
     * Order in the pickup sequence (set after route optimization).
     */
    @Column(name = "pickup_sequence")
    private Integer pickupSequence;

    /**
     * Calculate the direct distance on entity load.
     */
    @PostLoad
    public void calculateDirectDistance() {
        if (directDistanceKm == null && pickupLocation != null && dropoffLocation != null) {
            directDistanceKm = pickupLocation.distanceTo(dropoffLocation);
        }
    }

    /**
     * Check if the actual detour exceeds tolerance.
     */
    public boolean exceedsDetourTolerance() {
        if (directDistanceKm == null || directDistanceKm == 0 || actualDistanceKm == null) {
            return false;
        }
        double detourPercentage = (actualDistanceKm - directDistanceKm) / directDistanceKm;
        return detourPercentage > maxDetourTolerance;
    }
}
