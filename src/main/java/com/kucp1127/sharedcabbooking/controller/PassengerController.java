package com.kucp1127.sharedcabbooking.controller;

import com.kucp1127.sharedcabbooking.dto.request.PassengerRequest;
import com.kucp1127.sharedcabbooking.dto.response.ApiResponse;
import com.kucp1127.sharedcabbooking.dto.response.PassengerResponse;
import com.kucp1127.sharedcabbooking.service.PassengerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("passengers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "1. Passengers", description = "Step 1: Register passengers first")
public class PassengerController {

    private final PassengerService passengerService;

    @PostMapping
    @Operation(summary = "Register passenger", description = "Register a new passenger")
    public ResponseEntity<ApiResponse<PassengerResponse>> createPassenger(
            @Valid @RequestBody PassengerRequest request) {
        log.info("Creating passenger: {}", request.getEmail());
        PassengerResponse response = passengerService.createPassenger(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Passenger registered successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get passenger", description = "Get passenger by ID")
    public ResponseEntity<ApiResponse<PassengerResponse>> getPassenger(
            @Parameter(description = "Passenger ID") @PathVariable Long id) {
        PassengerResponse response = passengerService.getPassenger(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get passenger by email", description = "Get passenger by email address")
    public ResponseEntity<ApiResponse<PassengerResponse>> getPassengerByEmail(
            @Parameter(description = "Passenger email") @PathVariable String email) {
        PassengerResponse response = passengerService.getPassengerByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all passengers", description = "Get all registered passengers")
    public ResponseEntity<ApiResponse<List<PassengerResponse>>> getAllPassengers() {
        List<PassengerResponse> passengers = passengerService.getAllPassengers();
        return ResponseEntity.ok(ApiResponse.success(passengers));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update passenger", description = "Update passenger details")
    public ResponseEntity<ApiResponse<PassengerResponse>> updatePassenger(
            @Parameter(description = "Passenger ID") @PathVariable Long id,
            @Valid @RequestBody PassengerRequest request) {
        log.info("Updating passenger: {}", id);
        PassengerResponse response = passengerService.updatePassenger(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Passenger updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete passenger", description = "Delete a passenger")
    public ResponseEntity<ApiResponse<Void>> deletePassenger(
            @Parameter(description = "Passenger ID") @PathVariable Long id) {
        log.info("Deleting passenger: {}", id);
        passengerService.deletePassenger(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Passenger deleted successfully"));
    }
}
