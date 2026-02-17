package com.kucp1127.sharedcabbooking.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "passengers", indexes = {
    @Index(name = "idx_passenger_email", columnList = "email", unique = true),
    @Index(name = "idx_passenger_phone", columnList = "phone")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passenger extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    @Column(name = "phone", nullable = false)
    private String phone;
    @Column(name = "detour_tolerance")
    @Builder.Default
    private Double detourTolerance = 0.20;
    @Column(name = "preferred_cab_type")
    private String preferredCabType;
    @Column(name = "rating")
    @Builder.Default
    private Double rating = 5.0;
    @Column(name = "total_rides")
    @Builder.Default
    private Integer totalRides = 0;
}
