package com.kucp1127.sharedcabbooking.service.matching.impl;

import com.kucp1127.sharedcabbooking.domain.entity.Booking;
import com.kucp1127.sharedcabbooking.domain.entity.Location;
import com.kucp1127.sharedcabbooking.dto.MatchCandidate;
import com.kucp1127.sharedcabbooking.dto.request.RideRequest;
import com.kucp1127.sharedcabbooking.repository.BookingRepository;
import com.kucp1127.sharedcabbooking.service.matching.RideMatchingStrategy;
import com.kucp1127.sharedcabbooking.util.DistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Constraint-Based Clustering strategy for ride matching.
 *
 * Algorithm:
 * 1. Find pending bookings that satisfy ALL constraints
 * 2. Cluster by proximity and time
 * 3. Form optimal groups respecting detour tolerance
 *
 * This is used as a fallback or for proactive group formation.
 * Complexity: O(n * k) where n = pending bookings, k = constraints
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConstraintBasedClusteringStrategy implements RideMatchingStrategy {

    private final BookingRepository bookingRepository;
    private final DistanceCalculator distanceCalculator;

    @Value("${app.ride.matching.proximity-radius-km:5.0}")
    private double proximityRadiusKm;

    @Value("${app.ride.matching.time-window-minutes:30}")
    private int timeWindowMinutes;

    @Value("${app.ride.max-detour-tolerance:0.25}")
    private double maxDetourTolerance;

    @Override
    public List<MatchCandidate> findMatches(RideRequest request) {
        // This strategy is primarily for proactive clustering
        // For reactive matching, use GreedyNearestNeighbor
        log.debug("Constraint-based clustering is primarily for proactive group formation");
        return Collections.emptyList();
    }

    @Override
    public List<Booking> findCompatibleBookings(Booking booking) {
        log.debug("Finding compatible bookings for booking {} using constraint-based clustering",
                booking.getId());

        LocalDateTime timeStart = booking.getRequestedPickupTime().minusMinutes(timeWindowMinutes);
        LocalDateTime timeEnd = booking.getRequestedPickupTime().plusMinutes(timeWindowMinutes);

        // Step 1: Get all pending bookings in time window
        List<Booking> candidateBookings = bookingRepository.findNearbyPendingBookings(
                booking.getPickupLocation().getLatitude(),
                booking.getPickupLocation().getLongitude(),
                proximityRadiusKm,
                timeStart,
                timeEnd,
                50 // Consider more candidates for clustering
        );

        // Step 2: Filter by constraints
        List<Booking> compatibleBookings = new ArrayList<>();

        for (Booking candidate : candidateBookings) {
            if (candidate.getId().equals(booking.getId())) {
                continue;
            }

            if (areBookingsCompatible(booking, candidate)) {
                compatibleBookings.add(candidate);
            }
        }

        // Step 3: Sort by compatibility score
        compatibleBookings.sort((a, b) -> {
            double scoreA = calculateCompatibilityScore(booking, a);
            double scoreB = calculateCompatibilityScore(booking, b);
            return Double.compare(scoreB, scoreA);
        });

        log.debug("Found {} compatible bookings out of {} candidates",
                compatibleBookings.size(), candidateBookings.size());

        return compatibleBookings;
    }

    @Override
    public String getStrategyName() {
        return "CONSTRAINT_BASED_CLUSTERING";
    }

    @Override
    public int getPriority() {
        return 2; // Secondary/fallback strategy
    }

    /**
     * Check if two bookings are compatible for grouping.
     */
    private boolean areBookingsCompatible(Booking booking1, Booking booking2) {
        // Constraint 1: Proximity check
        double distance = distanceCalculator.calculateDistance(
                booking1.getPickupLocation(),
                booking2.getPickupLocation()
        );
        if (distance > proximityRadiusKm) {
            return false;
        }

        // Constraint 2: Time window check
        long timeDiffMinutes = Math.abs(java.time.Duration.between(
                booking1.getRequestedPickupTime(),
                booking2.getRequestedPickupTime()
        ).toMinutes());
        if (timeDiffMinutes > timeWindowMinutes) {
            return false;
        }

        // Constraint 3: Detour tolerance
        double minTolerance = Math.min(
                booking1.getMaxDetourTolerance(),
                booking2.getMaxDetourTolerance()
        );

        // Estimate detour if grouped
        Location dropoff1 = booking1.getDropoffLocation();
        Location dropoff2 = booking2.getDropoffLocation();

        // If both going to same destination (airport), minimal detour
        if (dropoff1 != null && dropoff2 != null) {
            double dropoffDistance = distanceCalculator.calculateDistance(dropoff1, dropoff2);
            if (dropoffDistance < 1.0) { // Same destination (within 1km)
                // Only consider pickup detour
                double pickupDetour = distance / booking1.getDirectDistanceKm();
                return pickupDetour <= minTolerance;
            }
        }

        // Constraint 4: Combined capacity check (basic - detailed check happens later)
        int totalPassengers = booking1.getPassengerCount() + booking2.getPassengerCount();
        if (totalPassengers > 6) { // Max for typical SUV
            return false;
        }

        double totalLuggage = booking1.getLuggageWeightKg() + booking2.getLuggageWeightKg();
        if (totalLuggage > 150) { // Max for typical SUV
            return false;
        }

        return true;
    }

    /**
     * Calculate compatibility score between two bookings.
     * Higher score = more compatible.
     */
    private double calculateCompatibilityScore(Booking booking1, Booking booking2) {
        double score = 100.0;

        // Factor 1: Distance penalty
        double distance = distanceCalculator.calculateDistance(
                booking1.getPickupLocation(),
                booking2.getPickupLocation()
        );
        score -= distance * 10;

        // Factor 2: Time difference penalty
        long timeDiffMinutes = Math.abs(java.time.Duration.between(
                booking1.getRequestedPickupTime(),
                booking2.getRequestedPickupTime()
        ).toMinutes());
        score -= timeDiffMinutes * 2;

        // Factor 3: Bonus for similar detour tolerances
        double toleranceDiff = Math.abs(
                booking1.getMaxDetourTolerance() - booking2.getMaxDetourTolerance()
        );
        score -= toleranceDiff * 50;

        // Factor 4: Bonus for similar luggage requirements
        double luggageDiff = Math.abs(
                booking1.getLuggageWeightKg() - booking2.getLuggageWeightKg()
        );
        score -= luggageDiff * 0.5;

        return Math.max(0, score);
    }
}
