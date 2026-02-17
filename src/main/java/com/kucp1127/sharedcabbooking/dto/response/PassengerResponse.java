package com.kucp1127.sharedcabbooking.dto.response;

import lombok.*;

/**
 * Response DTO for passenger information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private Double detourTolerance;
    private String preferredCabType;
    private Double rating;
    private Integer totalRides;
}
