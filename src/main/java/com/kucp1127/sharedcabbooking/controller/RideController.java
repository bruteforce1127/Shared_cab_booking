package com.kucp1127.sharedcabbooking.controller;

import com.kucp1127.sharedcabbooking.dto.request.RideRequest;
import com.kucp1127.sharedcabbooking.dto.response.ApiResponse;
import com.kucp1127.sharedcabbooking.dto.response.RideGroupResponse;
import com.kucp1127.sharedcabbooking.dto.response.RideResponse;
import com.kucp1127.sharedcabbooking.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rides")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "4. Rides", description = "Step 4: Book and manage rides")
public class RideController {

    private final RideService rideService;

    @PostMapping
    @Operation(summary = "Book a new ride", description = "Create a new ride booking and assign to optimal ride group")
    public ResponseEntity<ApiResponse<RideResponse>> bookRide(
            @Valid @RequestBody RideRequest request) {
        log.info("Received ride booking request for passenger: {}", request.getPassengerId());

        RideResponse response = rideService.bookRide(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Ride booked successfully"));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get ride details", description = "Get details of a specific ride/booking")
    public ResponseEntity<ApiResponse<RideResponse>> getRide(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId) {
        RideResponse response = rideService.getRide(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/passenger/{passengerId}")
    @Operation(summary = "Get passenger rides", description = "Get all rides for a specific passenger")
    public ResponseEntity<ApiResponse<List<RideResponse>>> getPassengerRides(
            @Parameter(description = "Passenger ID") @PathVariable Long passengerId) {
        List<RideResponse> rides = rideService.getPassengerRides(passengerId);
        return ResponseEntity.ok(ApiResponse.success(rides));
    }

    @GetMapping("/passenger/{passengerId}/paginated")
    @Operation(summary = "Get passenger rides (paginated)", description = "Get paginated rides for a specific passenger")
    public ResponseEntity<ApiResponse<Page<RideResponse>>> getPassengerRidesPaginated(
            @Parameter(description = "Passenger ID") @PathVariable Long passengerId,
            Pageable pageable) {
        Page<RideResponse> rides = rideService.getPassengerRidesPaginated(passengerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(rides));
    }

    @GetMapping("/passenger/{passengerId}/active")
    @Operation(summary = "Get active rides", description = "Get active rides for a specific passenger")
    public ResponseEntity<ApiResponse<List<RideResponse>>> getActiveRides(
            @Parameter(description = "Passenger ID") @PathVariable Long passengerId) {
        List<RideResponse> rides = rideService.getActiveRides(passengerId);
        return ResponseEntity.ok(ApiResponse.success(rides));
    }

    @GetMapping("/groups/{groupId}")
    @Operation(summary = "Get ride group", description = "Get details of a ride group")
    public ResponseEntity<ApiResponse<RideGroupResponse>> getRideGroup(
            @Parameter(description = "Ride Group ID") @PathVariable Long groupId) {
        RideGroupResponse response = rideService.getRideGroup(groupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{bookingId}/group")
    @Operation(summary = "Get ride group for booking", description = "Get the ride group associated with a booking")
    public ResponseEntity<ApiResponse<RideGroupResponse>> getRideGroupForBooking(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId) {
        RideGroupResponse response = rideService.getRideGroupForBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
