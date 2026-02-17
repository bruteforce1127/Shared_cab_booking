package com.kucp1127.sharedcabbooking.service.matching.impl;

import com.kucp1127.sharedcabbooking.domain.entity.Booking;
import com.kucp1127.sharedcabbooking.domain.entity.Location;
import com.kucp1127.sharedcabbooking.domain.entity.RideGroup;
import com.kucp1127.sharedcabbooking.domain.enums.RideGroupStatus;
import com.kucp1127.sharedcabbooking.dto.MatchCandidate;
import com.kucp1127.sharedcabbooking.dto.request.RideRequest;
import com.kucp1127.sharedcabbooking.repository.BookingRepository;
import com.kucp1127.sharedcabbooking.repository.RideGroupRepository;
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
 * Greedy Nearest Neighbor matching strategy.
 *
 * Algorithm:
 * 1. Find all forming ride groups within proximity radius
 * 2. Filter by time window compatibility
 * 3. Check capacity constraints (seats, luggage)
 * 4. Calculate detour for each candidate
 * 5. Score and rank candidates
 *
 * Complexity: O(n log n) with spatial filtering, where n = active groups
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GreedyNearestNeighborStrategy implements RideMatchingStrategy {

    private final RideGroupRepository rideGroupRepository;
    private final BookingRepository bookingRepository;
    private final DistanceCalculator distanceCalculator;

    @Value("${app.ride.matching.proximity-radius-km:5.0}")
    private double proximityRadiusKm;

    @Value("${app.ride.matching.time-window-minutes:30}")
    private int timeWindowMinutes;

    @Override
    public List<MatchCandidate> findMatches(RideRequest request) {
        log.debug("Finding matches using Greedy Nearest Neighbor strategy for pickup at ({}, {})",
                request.getPickupLatitude(), request.getPickupLongitude());

        LocalDateTime timeStart = request.getRequestedPickupTime().minusMinutes(timeWindowMinutes);
        LocalDateTime timeEnd = request.getRequestedPickupTime().plusMinutes(timeWindowMinutes);

        // Step 1: Find nearby forming groups using spatial query
        List<RideGroup> candidateGroups = rideGroupRepository.findMatchingRideGroups(
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                proximityRadiusKm,
                timeStart,
                timeEnd,
                20 // Limit candidates for performance
        );

        log.debug("Found {} candidate groups within {}km", candidateGroups.size(), proximityRadiusKm);

        // Step 2: Evaluate each candidate
        List<MatchCandidate> matches = new ArrayList<>();

        for (RideGroup group : candidateGroups) {
            MatchCandidate candidate = evaluateCandidate(group, request);
            if (candidate.getMeetsAllConstraints()) {
                matches.add(candidate);
            }
        }

        // Step 3: Sort by match score (descending)
        matches.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));

        log.debug("Returning {} valid matches", matches.size());
        return matches;
    }

    @Override
    public List<Booking> findCompatibleBookings(Booking booking) {
        LocalDateTime timeStart = booking.getRequestedPickupTime().minusMinutes(timeWindowMinutes);
        LocalDateTime timeEnd = booking.getRequestedPickupTime().plusMinutes(timeWindowMinutes);

        // Find nearby pending bookings
        List<Booking> nearbyBookings = bookingRepository.findNearbyPendingBookings(
                booking.getPickupLocation().getLatitude(),
                booking.getPickupLocation().getLongitude(),
                proximityRadiusKm,
                timeStart,
                timeEnd,
                10
        );

        // Filter out the current booking
        return nearbyBookings.stream()
                .filter(b -> !b.getId().equals(booking.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyName() {
        return "GREEDY_NEAREST_NEIGHBOR";
    }

    @Override
    public int getPriority() {
        return 1; // Primary strategy
    }

    /**
     * Evaluate a candidate group for matching.
     */
    private MatchCandidate evaluateCandidate(RideGroup group, RideRequest request) {
        List<String> violations = new ArrayList<>();
        boolean meetsConstraints = true;

        // Check seat capacity
        int requiredSeats = request.getPassengerCount();
        int availableSeats = group.getCab().getCabType().getMaxPassengers() - group.getTotalPassengers();
        if (availableSeats < requiredSeats) {
            violations.add("Insufficient seats: need " + requiredSeats + ", available " + availableSeats);
            meetsConstraints = false;
        }

        // Check luggage capacity
        double requiredLuggage = request.getLuggageWeightKg();
        double availableLuggage = group.getCab().getCabType().getMaxLuggageWeightKg() - group.getTotalLuggageWeightKg();
        if (availableLuggage < requiredLuggage) {
            violations.add("Insufficient luggage capacity: need " + requiredLuggage + "kg, available " + availableLuggage + "kg");
            meetsConstraints = false;
        }

        // Calculate detour
        Location newPickup = Location.builder()
                .latitude(request.getPickupLatitude())
                .longitude(request.getPickupLongitude())
                .build();

        List<Location> currentStops = extractPickupLocations(group);
        double additionalDistance = distanceCalculator.estimateInsertionCost(currentStops, newPickup);

        double currentTotalDistance = group.getTotalDistanceKm() != null ? group.getTotalDistanceKm() : 0.0;
        double newTotalDistance = currentTotalDistance + additionalDistance;
        double directDistance = group.getDirectDistanceKm() != null ? group.getDirectDistanceKm() :
                distanceCalculator.calculateDistance(newPickup, group.getAirportLocation());

        double detourPercentage = distanceCalculator.calculateDetourPercentage(directDistance, newTotalDistance);

        // Check detour tolerance for all existing passengers
        if (detourPercentage > request.getMaxDetourTolerance()) {
            violations.add("Detour exceeds tolerance: " + String.format("%.1f%%", detourPercentage * 100) +
                    " > " + String.format("%.1f%%", request.getMaxDetourTolerance() * 100));
            meetsConstraints = false;
        }

        // Calculate match score
        double matchScore = calculateMatchScore(group, request, additionalDistance, detourPercentage);

        return MatchCandidate.builder()
                .rideGroup(group)
                .matchScore(matchScore)
                .estimatedDetour(detourPercentage)
                .additionalDistance(additionalDistance)
                .meetsAllConstraints(meetsConstraints)
                .violatedConstraints(violations)
                .build();
    }

    /**
     * Calculate match score based on multiple factors.
     * Higher score = better match.
     */
    private double calculateMatchScore(RideGroup group, RideRequest request,
                                       double additionalDistance, double detourPercentage) {
        double score = 100.0;

        // Penalize additional distance (less distance = better)
        score -= additionalDistance * 5;

        // Penalize detour (less detour = better)
        score -= detourPercentage * 100;

        // Bonus for similar pickup times
        if (group.getScheduledDepartureTime() != null) {
            long timeDiffMinutes = Math.abs(
                    java.time.Duration.between(group.getScheduledDepartureTime(), request.getRequestedPickupTime()).toMinutes()
            );
            score -= timeDiffMinutes * 0.5;
        }

        // Bonus for groups with more passengers (better sharing discount)
        score += group.getTotalPassengers() * 2;

        return Math.max(0, score);
    }

    /**
     * Extract pickup locations from all bookings in the group.
     */
    private List<Location> extractPickupLocations(RideGroup group) {
        if (group.getBookings() == null || group.getBookings().isEmpty()) {
            return new ArrayList<>();
        }

        return group.getBookings().stream()
                .filter(b -> b.getPickupLocation() != null)
                .map(Booking::getPickupLocation)
                .collect(Collectors.toList());
    }
}
