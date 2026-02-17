package com.kucp1127.sharedcabbooking.service.matching;

import com.kucp1127.sharedcabbooking.domain.entity.*;
import com.kucp1127.sharedcabbooking.domain.enums.BookingStatus;
import com.kucp1127.sharedcabbooking.domain.enums.CabStatus;
import com.kucp1127.sharedcabbooking.domain.enums.RideGroupStatus;
import com.kucp1127.sharedcabbooking.dto.MatchCandidate;
import com.kucp1127.sharedcabbooking.dto.request.RideRequest;
import com.kucp1127.sharedcabbooking.repository.*;
import com.kucp1127.sharedcabbooking.util.DistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for grouping passengers into shared rides.
 * Implements Template Method pattern for grouping workflow.
 *
 * Workflow:
 * 1. Validate booking request
 * 2. Find matching ride groups
 * 3. Evaluate and score matches
 * 4. Assign to best group or create new group
 * 5. Optimize route
 * 6. Update pricing
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RideGroupingService {

    private final List<RideMatchingStrategy> matchingStrategies;
    private final RideGroupRepository rideGroupRepository;
    private final BookingRepository bookingRepository;
    private final CabRepository cabRepository;
    private final PassengerRepository passengerRepository;
    private final DistanceCalculator distanceCalculator;

    @Value("${app.ride.matching.proximity-radius-km:5.0}")
    private double proximityRadiusKm;

    @Value("${app.ride.max-detour-tolerance:0.25}")
    private double maxDetourTolerance;

    /**
     * Process a ride request and assign to optimal group.
     * This is the main entry point for ride matching.
     *
     * @param request The ride request
     * @return Created or updated booking
     */
    @Transactional
    public Booking processRideRequest(RideRequest request) {
        log.info("Processing ride request for passenger {}", request.getPassengerId());

        // Step 1: Validate request and get passenger
        Passenger passenger = validateAndGetPassenger(request);

        // Step 2: Create booking entity
        Booking booking = createBookingFromRequest(request, passenger);

        // Step 3: Find best matching group using strategies
        Optional<MatchCandidate> bestMatch = findBestMatch(request);

        if (bestMatch.isPresent() && bestMatch.get().getMeetsAllConstraints()) {
            // Step 4a: Add to existing group
            RideGroup group = bestMatch.get().getRideGroup();
            assignBookingToGroup(booking, group);
            log.info("Assigned booking {} to existing group {}", booking.getId(), group.getId());
        } else {
            // Step 4b: Create new group
            RideGroup newGroup = createNewRideGroup(booking, request);
            assignBookingToGroup(booking, newGroup);
            log.info("Created new group {} for booking {}", newGroup.getId(), booking.getId());
        }

        // Step 5: Update booking status
        booking.setStatus(BookingStatus.CONFIRMED);
        return bookingRepository.save(booking);
    }

    /**
     * Find the best matching group using configured strategies.
     * Strategies are evaluated in priority order.
     */
    private Optional<MatchCandidate> findBestMatch(RideRequest request) {
        // Sort strategies by priority
        List<RideMatchingStrategy> sortedStrategies = matchingStrategies.stream()
                .sorted(Comparator.comparingInt(RideMatchingStrategy::getPriority))
                .collect(Collectors.toList());

        for (RideMatchingStrategy strategy : sortedStrategies) {
            log.debug("Trying matching strategy: {}", strategy.getStrategyName());
            List<MatchCandidate> matches = strategy.findMatches(request);

            if (!matches.isEmpty()) {
                // Return the best match from first strategy that finds matches
                MatchCandidate best = matches.get(0);
                log.debug("Found match using {} with score {}",
                        strategy.getStrategyName(), best.getMatchScore());
                return Optional.of(best);
            }
        }

        log.debug("No matches found by any strategy");
        return Optional.empty();
    }

    /**
     * Validate request and retrieve passenger.
     */
    private Passenger validateAndGetPassenger(RideRequest request) {
        return passengerRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Passenger not found: " + request.getPassengerId()));
    }

    /**
     * Create booking entity from request.
     */
    private Booking createBookingFromRequest(RideRequest request, Passenger passenger) {
        Location pickupLocation = Location.builder()
                .latitude(request.getPickupLatitude())
                .longitude(request.getPickupLongitude())
                .address(request.getPickupAddress())
                .build();

        Location dropoffLocation = Location.builder()
                .latitude(request.getDropoffLatitude())
                .longitude(request.getDropoffLongitude())
                .address(request.getDropoffAddress())
                .build();

        double directDistance = distanceCalculator.calculateDistance(pickupLocation, dropoffLocation);

        Booking booking = Booking.builder()
                .passenger(passenger)
                .pickupLocation(pickupLocation)
                .dropoffLocation(dropoffLocation)
                .requestedPickupTime(request.getRequestedPickupTime())
                .passengerCount(request.getPassengerCount())
                .luggageWeightKg(request.getLuggageWeightKg())
                .luggageCount(request.getLuggageCount())
                .maxDetourTolerance(request.getMaxDetourTolerance())
                .directDistanceKm(directDistance)
                .specialRequirements(request.getSpecialRequirements())
                .status(BookingStatus.PENDING)
                .build();

        return bookingRepository.save(booking);
    }

    /**
     * Create a new ride group with an available cab.
     */
    private RideGroup createNewRideGroup(Booking booking, RideRequest request) {
        // Find suitable cab
        Cab cab = findSuitableCab(request);

        RideGroup group = RideGroup.builder()
                .cab(cab)
                .status(RideGroupStatus.FORMING)
                .airportLocation(booking.getDropoffLocation())
                .scheduledDepartureTime(request.getRequestedPickupTime())
                .totalPassengers(0)
                .totalLuggageWeightKg(0.0)
                .directDistanceKm(booking.getDirectDistanceKm())
                .build();

        // Update cab status
        cab.setStatus(CabStatus.ASSIGNED);
        cabRepository.save(cab);

        return rideGroupRepository.save(group);
    }

    /**
     * Find a suitable cab for the ride request.
     */
    private Cab findSuitableCab(RideRequest request) {
        String cabType = request.getPreferredCabType();
        if (cabType == null || cabType.isEmpty()) {
            cabType = "SEDAN"; // Default
        }

        List<Cab> availableCabs = cabRepository.findSuitableCabsNearLocation(
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                proximityRadiusKm * 2, // Wider radius for cab search
                cabType,
                request.getPassengerCount(),
                request.getLuggageWeightKg(),
                1
        );

        if (availableCabs.isEmpty()) {
            // Fallback: find any available cab
            availableCabs = cabRepository.findAvailableCabsNearLocation(
                    request.getPickupLatitude(),
                    request.getPickupLongitude(),
                    proximityRadiusKm * 3,
                    5
            );
        }

        if (availableCabs.isEmpty()) {
            throw new IllegalStateException("No available cabs found");
        }

        return availableCabs.get(0);
    }

    /**
     * Assign a booking to a ride group and optimize route.
     */
    @Transactional
    public void assignBookingToGroup(Booking booking, RideGroup group) {
        // Add booking to group
        group.addBooking(booking);
        booking.setRideGroup(group);

        // Recalculate route and distance
        optimizeGroupRoute(group);

        // Update booking sequence
        updatePickupSequences(group);

        // Save updates
        rideGroupRepository.save(group);
        bookingRepository.save(booking);
    }

    /**
     * Optimize the pickup route for a group.
     * Uses nearest neighbor heuristic for TSP approximation.
     * Complexity: O(n²) where n = number of pickups
     */
    private void optimizeGroupRoute(RideGroup group) {
        List<Booking> bookings = group.getBookings();
        if (bookings.size() <= 1) {
            if (!bookings.isEmpty()) {
                group.setTotalDistanceKm(bookings.get(0).getDirectDistanceKm());
                group.setOptimizedRoute(String.valueOf(bookings.get(0).getId()));
            }
            return;
        }

        // Extract pickup locations
        List<Location> pickupLocations = bookings.stream()
                .map(Booking::getPickupLocation)
                .collect(Collectors.toList());

        // Add airport as final destination
        pickupLocations.add(group.getAirportLocation());

        // Nearest neighbor TSP approximation
        List<Integer> optimizedOrder = nearestNeighborTSP(pickupLocations);

        // Calculate total distance
        double totalDistance = 0;
        for (int i = 0; i < optimizedOrder.size() - 1; i++) {
            totalDistance += distanceCalculator.calculateDistance(
                    pickupLocations.get(optimizedOrder.get(i)),
                    pickupLocations.get(optimizedOrder.get(i + 1))
            );
        }

        // Store optimized route
        String routeStr = optimizedOrder.stream()
                .filter(i -> i < bookings.size()) // Exclude airport
                .map(i -> String.valueOf(bookings.get(i).getId()))
                .collect(Collectors.joining(","));

        group.setOptimizedRoute(routeStr);
        group.setTotalDistanceKm(totalDistance);
    }

    /**
     * Nearest Neighbor TSP heuristic.
     * Complexity: O(n²)
     */
    private List<Integer> nearestNeighborTSP(List<Location> locations) {
        int n = locations.size();
        List<Integer> route = new ArrayList<>();
        boolean[] visited = new boolean[n];

        // Start from first pickup
        int current = 0;
        route.add(current);
        visited[current] = true;

        while (route.size() < n) {
            double minDistance = Double.MAX_VALUE;
            int nearest = -1;

            for (int i = 0; i < n; i++) {
                if (!visited[i]) {
                    double distance = distanceCalculator.calculateDistance(
                            locations.get(current),
                            locations.get(i)
                    );
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearest = i;
                    }
                }
            }

            if (nearest != -1) {
                route.add(nearest);
                visited[nearest] = true;
                current = nearest;
            }
        }

        return route;
    }

    /**
     * Update pickup sequences based on optimized route.
     */
    private void updatePickupSequences(RideGroup group) {
        String routeStr = group.getOptimizedRoute();
        if (routeStr == null || routeStr.isEmpty()) {
            return;
        }

        String[] bookingIds = routeStr.split(",");
        Map<Long, Integer> sequenceMap = new HashMap<>();

        for (int i = 0; i < bookingIds.length; i++) {
            sequenceMap.put(Long.parseLong(bookingIds[i]), i + 1);
        }

        for (Booking booking : group.getBookings()) {
            Integer sequence = sequenceMap.get(booking.getId());
            if (sequence != null) {
                booking.setPickupSequence(sequence);
            }
        }
    }

    /**
     * Remove a booking from a group (for cancellations).
     */
    @Transactional
    public void removeBookingFromGroup(Booking booking) {
        RideGroup group = booking.getRideGroup();
        if (group == null) {
            return;
        }

        group.removeBooking(booking);
        booking.setRideGroup(null);
        booking.setPickupSequence(null);

        // Re-optimize route if group still has bookings
        if (!group.getBookings().isEmpty()) {
            optimizeGroupRoute(group);
            updatePickupSequences(group);
            rideGroupRepository.save(group);
        } else {
            // Cancel group if empty
            group.setStatus(RideGroupStatus.CANCELLED);
            if (group.getCab() != null) {
                group.getCab().setStatus(CabStatus.AVAILABLE);
                cabRepository.save(group.getCab());
            }
            rideGroupRepository.save(group);
        }

        bookingRepository.save(booking);
    }
}
