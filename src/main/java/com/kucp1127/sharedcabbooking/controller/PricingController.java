package com.kucp1127.sharedcabbooking.controller;

import com.kucp1127.sharedcabbooking.dto.request.RideRequest;
import com.kucp1127.sharedcabbooking.dto.response.ApiResponse;
import com.kucp1127.sharedcabbooking.dto.response.FareEstimateResponse;
import com.kucp1127.sharedcabbooking.service.pricing.PricingEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("pricing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "3. Pricing", description = "Step 3: Get fare estimates")
public class PricingController {

    private final PricingEngine pricingEngine;

    @PostMapping("/estimate")
    @Operation(summary = "Get fare estimate", description = "Calculate estimated fare for a ride")
    public ResponseEntity<ApiResponse<FareEstimateResponse>> getFareEstimate(
            @Valid @RequestBody RideRequest request) {
        log.info("Calculating fare estimate for pickup ({}, {}) to ({}, {})",
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDropoffLatitude(), request.getDropoffLongitude());

        FareEstimateResponse estimate = pricingEngine.calculateFareEstimate(request);

        return ResponseEntity.ok(ApiResponse.success(estimate, "Fare estimate calculated"));
    }

    @GetMapping("/surge")
    @Operation(summary = "Get current surge multiplier", description = "Get the current surge pricing multiplier")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentSurge() {
        Double surgeMultiplier = pricingEngine.getCurrentSurgeMultiplier();

        Map<String, Object> response = Map.of(
                "surgeMultiplier", surgeMultiplier,
                "surgePercentage", (surgeMultiplier - 1.0) * 100,
                "surgeActive", surgeMultiplier > 1.0
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
