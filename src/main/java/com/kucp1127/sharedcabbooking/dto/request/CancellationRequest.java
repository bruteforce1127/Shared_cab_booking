package com.kucp1127.sharedcabbooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    private String reason;

    @NotBlank(message = "Initiator is required")
    @Builder.Default
    private String initiatedBy = "PASSENGER";
}
