package com.kucp1127.sharedcabbooking.controller;

import com.kucp1127.sharedcabbooking.domain.entity.Cancellation;
import com.kucp1127.sharedcabbooking.dto.request.CancellationRequest;
import com.kucp1127.sharedcabbooking.dto.response.ApiResponse;
import com.kucp1127.sharedcabbooking.service.booking.CancellationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "5. Bookings", description = "Step 5: Manage bookings (cancel, etc.)")
public class BookingController {

    private final CancellationService cancellationService;

    @PostMapping("/cancel")
    @Operation(summary = "Cancel booking", description = "Cancel a booking and process refund")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelBooking(
            @Valid @RequestBody CancellationRequest request) {
        log.info("Received cancellation request for booking: {}", request.getBookingId());

        Cancellation cancellation = cancellationService.cancelBooking(request);

        Map<String, Object> result = Map.of(
                "cancellationId", cancellation.getId(),
                "bookingId", request.getBookingId(),
                "cancellationFee", cancellation.getCancellationFee(),
                "refundAmount", cancellation.getRefundAmount(),
                "cancelledAt", cancellation.getCancelledAt()
        );

        return ResponseEntity.ok(ApiResponse.success(result, "Booking cancelled successfully"));
    }

    @GetMapping("/{bookingId}/can-cancel")
    @Operation(summary = "Check if booking can be cancelled", description = "Check cancellation eligibility")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> canCancelBooking(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId) {
        boolean canCancel = cancellationService.canCancel(bookingId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("canCancel", canCancel)));
    }
}
