package com.kucp1127.sharedcabbooking.domain.entity;

import com.kucp1127.sharedcabbooking.domain.enums.RideGroupStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ride_groups", indexes = {
    @Index(name = "idx_ride_group_status", columnList = "status"),
    @Index(name = "idx_ride_group_status_created", columnList = "status, created_at"),
    @Index(name = "idx_ride_group_departure_time", columnList = "scheduled_departure_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideGroup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cab_id")
    private Cab cab;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RideGroupStatus status = RideGroupStatus.FORMING;

    /**
     * Airport location (common destination for airport rides).
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude", column = @Column(name = "airport_latitude")),
        @AttributeOverride(name = "longitude", column = @Column(name = "airport_longitude")),
        @AttributeOverride(name = "address", column = @Column(name = "airport_address"))
    })
    private Location airportLocation;

    /**
     * Scheduled departure time for the group.
     */
    @Column(name = "scheduled_departure_time")
    private LocalDateTime scheduledDepartureTime;

    /**
     * Actual departure time when ride started.
     */
    @Column(name = "actual_departure_time")
    private LocalDateTime actualDepartureTime;

    /**
     * Estimated arrival time at airport.
     */
    @Column(name = "estimated_arrival_time")
    private LocalDateTime estimatedArrivalTime;

    /**
     * Total passengers currently in this group.
     */
    @Column(name = "total_passengers")
    @Builder.Default
    private Integer totalPassengers = 0;

    /**
     * Total luggage weight in kg for this group.
     */
    @Column(name = "total_luggage_weight_kg")
    @Builder.Default
    private Double totalLuggageWeightKg = 0.0;

    /**
     * Optimized route as ordered list of booking IDs.
     * Stored as comma-separated values for simplicity.
     */
    @Column(name = "optimized_route", columnDefinition = "TEXT")
    private String optimizedRoute;

    /**
     * Total estimated distance of the optimized route in km.
     */
    @Column(name = "total_distance_km")
    private Double totalDistanceKm;

    /**
     * Direct distance to airport (baseline for detour calculation).
     */
    @Column(name = "direct_distance_km")
    private Double directDistanceKm;

    /**
     * Bookings in this ride group.
     */
    @OneToMany(mappedBy = "rideGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    /**
     * Calculate detour percentage for this group.
     */
    public double getDetourPercentage() {
        if (directDistanceKm == null || directDistanceKm == 0 || totalDistanceKm == null) {
            return 0.0;
        }
        return (totalDistanceKm - directDistanceKm) / directDistanceKm;
    }

    /**
     * Check if a new booking can be added respecting constraints.
     */
    public boolean canAddBooking(int passengerCount, double luggageWeightKg, double maxDetourTolerance) {
        if (status != RideGroupStatus.FORMING) {
            return false;
        }

        if (cab == null) {
            return false;
        }

        // Check seat capacity
        int newTotalPassengers = totalPassengers + passengerCount;
        if (newTotalPassengers > cab.getCabType().getMaxPassengers()) {
            return false;
        }

        // Check luggage capacity
        double newTotalLuggage = totalLuggageWeightKg + luggageWeightKg;
        if (newTotalLuggage > cab.getCabType().getMaxLuggageWeightKg()) {
            return false;
        }

        return true;
    }

    /**
     * Add a booking to this group.
     */
    public void addBooking(Booking booking) {
        bookings.add(booking);
        booking.setRideGroup(this);
        totalPassengers += booking.getPassengerCount();
        totalLuggageWeightKg += booking.getLuggageWeightKg();
    }

    /**
     * Remove a booking from this group.
     */
    public void removeBooking(Booking booking) {
        bookings.remove(booking);
        booking.setRideGroup(null);
        totalPassengers -= booking.getPassengerCount();
        totalLuggageWeightKg -= booking.getLuggageWeightKg();
    }
}
