package com.kucp1127.sharedcabbooking.service.booking;

import com.kucp1127.sharedcabbooking.domain.entity.Booking;
import com.kucp1127.sharedcabbooking.domain.entity.RideGroup;
import com.kucp1127.sharedcabbooking.domain.enums.BookingStatus;
import com.kucp1127.sharedcabbooking.domain.enums.CabStatus;
import com.kucp1127.sharedcabbooking.domain.enums.RideGroupStatus;
import com.kucp1127.sharedcabbooking.event.BookingCancelledEvent;
import com.kucp1127.sharedcabbooking.event.RideGroupRebalanceEvent;
import com.kucp1127.sharedcabbooking.repository.BookingRepository;
import com.kucp1127.sharedcabbooking.repository.CabRepository;
import com.kucp1127.sharedcabbooking.repository.RideGroupRepository;
import com.kucp1127.sharedcabbooking.service.locking.DistributedLockService;
import com.kucp1127.sharedcabbooking.service.matching.RideGroupingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for rebalancing ride groups after cancellations or other changes.
 * Implements Observer pattern as event listener.
 *
 * Rebalancing Strategy:
 * 1. If group becomes empty - cancel group
 * 2. If group has insufficient passengers - try to merge with another group
 * 3. Re-optimize route for remaining passengers
 * 4. Update estimated pickup times
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RideGroupRebalancer {

    private final RideGroupRepository rideGroupRepository;
    private final BookingRepository bookingRepository;
    private final CabRepository cabRepository;
    private final DistributedLockService lockService;

    private static final int MIN_PASSENGERS_FOR_DISPATCH = 1;

    /**
     * Handle booking cancellation event.
     * Async processing to not block the cancellation flow.
     */
    @Async
    @EventListener
    public void handleBookingCancelled(BookingCancelledEvent event) {
        log.info("Handling cancellation event for booking: {}, ride group: {}",
                event.getBookingId(), event.getRideGroupId());

        if (event.getRideGroupId() == null) {
            log.debug("No ride group associated with cancelled booking");
            return;
        }

        try {
            rebalanceRideGroup(event.getRideGroupId(), "Booking cancelled: " + event.getBookingId());
        } catch (Exception e) {
            log.error("Error rebalancing ride group {} after cancellation: {}",
                    event.getRideGroupId(), e.getMessage(), e);
        }
    }

    /**
     * Handle explicit rebalance requests.
     */
    @Async
    @EventListener
    public void handleRebalanceRequest(RideGroupRebalanceEvent event) {
        log.info("Handling rebalance request for ride group: {}", event.getRideGroupId());

        try {
            rebalanceRideGroup(event.getRideGroupId(), event.getReason());
        } catch (Exception e) {
            log.error("Error rebalancing ride group {}: {}",
                    event.getRideGroupId(), e.getMessage(), e);
        }
    }

    /**
     * Rebalance a ride group with distributed lock.
     */
    @Transactional
    public void rebalanceRideGroup(Long rideGroupId, String reason) {
        lockService.executeWithRideGroupLock(rideGroupId, () -> {
            performRebalance(rideGroupId, reason);
            return null;
        });
    }

    /**
     * Perform the actual rebalancing logic.
     */
    private void performRebalance(Long rideGroupId, String reason) {
        RideGroup group = rideGroupRepository.findByIdWithLock(rideGroupId).orElse(null);

        if (group == null) {
            log.warn("Ride group not found for rebalancing: {}", rideGroupId);
            return;
        }

        // Skip if group is already in terminal state
        if (group.getStatus() == RideGroupStatus.COMPLETED ||
            group.getStatus() == RideGroupStatus.CANCELLED) {
            log.debug("Skipping rebalance for terminal group: {}", rideGroupId);
            return;
        }

        log.info("Rebalancing ride group: {}, reason: {}, current passengers: {}",
                rideGroupId, reason, group.getTotalPassengers());

        // Get active bookings in the group
        List<Booking> activeBookings = group.getBookings().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED ||
                             b.getStatus() == BookingStatus.IN_PROGRESS)
                .toList();

        if (activeBookings.isEmpty()) {
            // Cancel the group if no active bookings
            cancelEmptyGroup(group);
        } else if (activeBookings.size() < MIN_PASSENGERS_FOR_DISPATCH &&
                   group.getStatus() == RideGroupStatus.FORMING) {
            // Try to find merge opportunities
            tryMergeWithOtherGroups(group, activeBookings);
        } else {
            // Just re-optimize the route
            updateGroupAfterRebalance(group);
        }
    }

    /**
     * Cancel an empty ride group and release the cab.
     */
    private void cancelEmptyGroup(RideGroup group) {
        log.info("Cancelling empty ride group: {}", group.getId());

        group.setStatus(RideGroupStatus.CANCELLED);

        // Release the cab
        if (group.getCab() != null) {
            group.getCab().setStatus(CabStatus.AVAILABLE);
            cabRepository.save(group.getCab());
        }

        rideGroupRepository.save(group);
    }

    /**
     * Try to merge bookings with another forming group.
     */
    private void tryMergeWithOtherGroups(RideGroup sourceGroup, List<Booking> bookings) {
        log.debug("Attempting to merge group {} with {} bookings",
                sourceGroup.getId(), bookings.size());

        // For simplicity, just update the group
        // A full implementation would search for compatible groups
        updateGroupAfterRebalance(sourceGroup);

        // TODO: Implement full merge logic:
        // 1. Find forming groups with similar destination and time
        // 2. Check capacity constraints
        // 3. Move bookings to target group
        // 4. Cancel source group
    }

    /**
     * Update group state after rebalancing.
     */
    private void updateGroupAfterRebalance(RideGroup group) {
        // Recalculate totals
        int totalPassengers = 0;
        double totalLuggage = 0.0;

        for (Booking booking : group.getBookings()) {
            if (booking.getStatus() == BookingStatus.CONFIRMED ||
                booking.getStatus() == BookingStatus.IN_PROGRESS) {
                totalPassengers += booking.getPassengerCount();
                totalLuggage += booking.getLuggageWeightKg();
            }
        }

        group.setTotalPassengers(totalPassengers);
        group.setTotalLuggageWeightKg(totalLuggage);

        // Route optimization is handled by RideGroupingService
        // Here we just update the state

        rideGroupRepository.save(group);
        log.info("Updated ride group {} after rebalance: {} passengers, {} kg luggage",
                group.getId(), totalPassengers, totalLuggage);
    }
}
