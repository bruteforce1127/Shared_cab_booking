package com.kucp1127.sharedcabbooking.dto.request;

import com.kucp1127.sharedcabbooking.domain.enums.CabType;
import jakarta.validation.constraints.*;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CabRequest {

    @NotBlank(message = "License plate is required")
    @Pattern(regexp = "^[A-Z0-9-]{5,15}$", message = "Invalid license plate format")
    private String licensePlate;

    @NotBlank(message = "Driver name is required")
    @Size(min = 2, max = 255, message = "Driver name must be between 2 and 255 characters")
    private String driverName;

    @NotBlank(message = "Driver phone is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
    private String driverPhone;

    @NotNull(message = "Cab type is required")
    private CabType cabType;

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private Double currentLatitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private Double currentLongitude;

    private String currentAddress;
}
