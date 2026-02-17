package com.kucp1127.sharedcabbooking.domain.entity;

import com.kucp1127.sharedcabbooking.domain.enums.CabStatus;
import com.kucp1127.sharedcabbooking.domain.enums.CabType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cabs", indexes = {
    @Index(name = "idx_cab_status", columnList = "status"),
    @Index(name = "idx_cab_type_status", columnList = "cab_type, status"),
    @Index(name = "idx_cab_license", columnList = "license_plate", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cab extends BaseEntity {

    @Column(name = "license_plate", nullable = false, unique = true)
    private String licensePlate;

    @Column(name = "driver_name", nullable = false)
    private String driverName;

    @Column(name = "driver_phone", nullable = false)
    private String driverPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "cab_type", nullable = false)
    private CabType cabType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CabStatus status = CabStatus.AVAILABLE;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude", column = @Column(name = "current_latitude")),
        @AttributeOverride(name = "longitude", column = @Column(name = "current_longitude")),
        @AttributeOverride(name = "address", column = @Column(name = "current_address"))
    })
    private Location currentLocation;

    @Column(name = "driver_rating")
    @Builder.Default
    private Double driverRating = 5.0;

    /**
     * Current available seats (changes as passengers board/exit).
     */
    @Column(name = "available_seats")
    private Integer availableSeats;

    /**
     * Current available luggage capacity in kg.
     */
    @Column(name = "available_luggage_capacity_kg")
    private Double availableLuggageCapacityKg;

    @PostPersist
    @PostLoad
    public void initializeCapacity() {
        if (availableSeats == null) {
            availableSeats = cabType.getMaxPassengers();
        }
        if (availableLuggageCapacityKg == null) {
            availableLuggageCapacityKg = cabType.getMaxLuggageWeightKg();
        }
    }

    public boolean canAccommodate(int passengers, double luggageWeightKg) {
        return availableSeats >= passengers && availableLuggageCapacityKg >= luggageWeightKg;
    }
}
