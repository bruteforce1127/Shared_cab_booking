package com.kucp1127.sharedcabbooking.controller;

import com.kucp1127.sharedcabbooking.domain.enums.CabStatus;
import com.kucp1127.sharedcabbooking.domain.enums.CabType;
import com.kucp1127.sharedcabbooking.dto.request.CabRequest;
import com.kucp1127.sharedcabbooking.dto.response.ApiResponse;
import com.kucp1127.sharedcabbooking.dto.response.CabResponse;
import com.kucp1127.sharedcabbooking.service.CabService;
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
@RequestMapping("/cabs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "2. Cabs", description = "Step 2: Register cabs/drivers")
public class CabController {

    private final CabService cabService;

    @PostMapping
    @Operation(summary = "Register cab", description = "Register a new cab/driver")
    public ResponseEntity<ApiResponse<CabResponse>> registerCab(
            @Valid @RequestBody CabRequest request) {
        log.info("Registering cab: {}", request.getLicensePlate());
        CabResponse response = cabService.registerCab(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Cab registered successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get cab", description = "Get cab by ID")
    public ResponseEntity<ApiResponse<CabResponse>> getCab(
            @Parameter(description = "Cab ID") @PathVariable Long id) {
        CabResponse response = cabService.getCab(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/license/{licensePlate}")
    @Operation(summary = "Get cab by license", description = "Get cab by license plate")
    public ResponseEntity<ApiResponse<CabResponse>> getCabByLicensePlate(
            @Parameter(description = "License plate") @PathVariable String licensePlate) {
        CabResponse response = cabService.getCabByLicensePlate(licensePlate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all cabs", description = "Get all registered cabs")
    public ResponseEntity<ApiResponse<List<CabResponse>>> getAllCabs() {
        List<CabResponse> cabs = cabService.getAllCabs();
        return ResponseEntity.ok(ApiResponse.success(cabs));
    }

    @GetMapping("/available")
    @Operation(summary = "Get available cabs", description = "Get all available cabs")
    public ResponseEntity<ApiResponse<List<CabResponse>>> getAvailableCabs() {
        List<CabResponse> cabs = cabService.getAvailableCabs();
        return ResponseEntity.ok(ApiResponse.success(cabs));
    }

    @GetMapping("/available/type/{cabType}")
    @Operation(summary = "Get available cabs by type", description = "Get available cabs of specific type")
    public ResponseEntity<ApiResponse<List<CabResponse>>> getAvailableCabsByType(
            @Parameter(description = "Cab type") @PathVariable CabType cabType) {
        List<CabResponse> cabs = cabService.getAvailableCabsByType(cabType);
        return ResponseEntity.ok(ApiResponse.success(cabs));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Get nearby cabs", description = "Get available cabs near a location")
    public ResponseEntity<ApiResponse<List<CabResponse>>> getNearbyCabs(
            @Parameter(description = "Latitude") @RequestParam Double latitude,
            @Parameter(description = "Longitude") @RequestParam Double longitude,
            @Parameter(description = "Search radius in km") @RequestParam(defaultValue = "5.0") Double radiusKm) {
        List<CabResponse> cabs = cabService.getNearbyCabs(latitude, longitude, radiusKm);
        return ResponseEntity.ok(ApiResponse.success(cabs));
    }

    @PatchMapping("/{id}/location")
    @Operation(summary = "Update cab location", description = "Update cab's current location")
    public ResponseEntity<ApiResponse<CabResponse>> updateCabLocation(
            @Parameter(description = "Cab ID") @PathVariable Long id,
            @Parameter(description = "Latitude") @RequestParam Double latitude,
            @Parameter(description = "Longitude") @RequestParam Double longitude,
            @Parameter(description = "Address") @RequestParam(required = false) String address) {
        CabResponse response = cabService.updateCabLocation(id, latitude, longitude, address);
        return ResponseEntity.ok(ApiResponse.success(response, "Location updated"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update cab status", description = "Update cab's availability status")
    public ResponseEntity<ApiResponse<CabResponse>> updateCabStatus(
            @Parameter(description = "Cab ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam CabStatus status) {
        CabResponse response = cabService.updateCabStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Status updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete cab", description = "Delete a cab")
    public ResponseEntity<ApiResponse<Void>> deleteCab(
            @Parameter(description = "Cab ID") @PathVariable Long id) {
        log.info("Deleting cab: {}", id);
        cabService.deleteCab(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Cab deleted successfully"));
    }
}
