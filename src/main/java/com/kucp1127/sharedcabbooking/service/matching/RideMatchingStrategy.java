package com.kucp1127.sharedcabbooking.service.matching;

import com.kucp1127.sharedcabbooking.domain.entity.Booking;
import com.kucp1127.sharedcabbooking.dto.MatchCandidate;
import com.kucp1127.sharedcabbooking.dto.request.RideRequest;

import java.util.List;

/**
 * Strategy interface for ride matching algorithms.
 * Implements Strategy Pattern - allows swapping matching algorithms at runtime.
 *
 * Complexity varies by implementation:
 * - GreedyNearestNeighbor: O(n log n) with spatial indexing
 * - ConstraintBasedClustering: O(n * k) where k = constraints
 */
public interface RideMatchingStrategy {

    /**
     * Find candidate ride groups for a new booking request.
     *
     * @param request The new ride request to match
     * @return List of candidate matches sorted by score (best first)
     */
    List<MatchCandidate> findMatches(RideRequest request);

    /**
     * Find candidate bookings that can be grouped with existing booking.
     * Used for proactive group formation.
     *
     * @param booking Existing booking to find matches for
     * @return List of compatible bookings
     */
    List<Booking> findCompatibleBookings(Booking booking);

    /**
     * Get the name of this matching strategy.
     */
    String getStrategyName();

    /**
     * Get the priority of this strategy (lower = higher priority).
     */
    default int getPriority() {
        return 100;
    }
}
