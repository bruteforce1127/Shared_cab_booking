package com.kucp1127.sharedcabbooking.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
    private String phone;

    @DecimalMin(value = "0.0", message = "Detour tolerance must be >= 0")
    @DecimalMax(value = "0.5", message = "Detour tolerance must be <= 50%")
    @Builder.Default
    private Double detourTolerance = 0.20;

    private String preferredCabType;
}
