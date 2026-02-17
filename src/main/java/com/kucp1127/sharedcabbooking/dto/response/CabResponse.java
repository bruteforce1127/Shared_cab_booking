package com.kucp1127.sharedcabbooking.dto.response;

import com.kucp1127.sharedcabbooking.domain.enums.CabStatus;
import com.kucp1127.sharedcabbooking.domain.enums.CabType;
import lombok.*;

/**
 * Response DTO for cab information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CabResponse {

    private Long id;
    private String licensePlate;
    private String driverName;
    private String driverPhone;
    private CabType cabType;
    private CabStatus status;
    private Double currentLatitude;
    private Double currentLongitude;
    private String currentAddress;
    private Double driverRating;
    private Integer availableSeats;
    private Double availableLuggageCapacityKg;
}
