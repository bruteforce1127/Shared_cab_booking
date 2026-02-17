package com.kucp1127.sharedcabbooking.service.booking;

import com.kucp1127.sharedcabbooking.domain.entity.Booking;
import com.kucp1127.sharedcabbooking.domain.entity.Cancellation;
import com.kucp1127.sharedcabbooking.domain.entity.RideGroup;
import com.kucp1127.sharedcabbooking.domain.enums.BookingStatus;
import com.kucp1127.sharedcabbooking.dto.request.CancellationRequest;
import com.kucp1127.sharedcabbooking.event.BookingCancelledEvent;
import com.kucp1127.sharedcabbooking.exception.CancellationException;
import com.kucp1127.sharedcabbooking.exception.ResourceNotFoundException;
import com.kucp1127.sharedcabbooking.repository.BookingRepository;
import com.kucp1127.sharedcabbooking.repository.CancellationRepository;
import com.kucp1127.sharedcabbooking.service.locking.DistributedLockService;
import com.kucp1127.sharedcabbooking.service.matching.RideGroupingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;


@Service
@Slf4j
@RequiredArgsConstructor
public class CancellationService {

    private final BookingRepository bookingRepository;
    private final CancellationRepository cancellationRepository;
    private final RideGroupingService rideGroupingService;
    private final DistributedLockService lockService;
    private final ApplicationEventPublisher eventPublisher;

    // Cancellation fee thresholds
    private static final int FREE_CANCELLATION_MINUTES = 10;
    private static final BigDecimal CANCELLATION_FEE_PERCENTAGE = new BigDecimal("0.20"); // 20%

    // Statuses that can be cancelled
    private static final Set<BookingStatus> CANCELLABLE_STATUSES = Set.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED
    );

    /**
     * Cancel a booking with distributed lock protection.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Cancellation cancelBooking(CancellationRequest request) {
        log.info("Processing cancellation request for booking: {}", request.getBookingId());

        return lockService.executeWithBookingLock(request.getBookingId(), () ->
            processCancellation(request)
        );
    }

    private Cancellation processCancellation(CancellationRequest request) {
        // Fetch booking with lock
        Booking booking = bookingRepository.findByIdWithLock(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", request.getBookingId()));

        // Validate cancellation is allowed
        validateCancellation(booking);

        // Calculate fees
        BigDecimal cancellationFee = calculateCancellationFee(booking);
        BigDecimal refundAmount = calculateRefundAmount(booking, cancellationFee);

        // Get ride group info before removal
        Long rideGroupId = booking.getRideGroup() != null ? booking.getRideGroup().getId() : null;
        Long passengerId = booking.getPassenger().getId();

        // Create cancellation record
        Cancellation cancellation = Cancellation.builder()
                .booking(booking)
                .cancelledAt(LocalDateTime.now())
                .reason(request.getReason())
                .initiatedBy(request.getInitiatedBy())
                .cancellationFee(cancellationFee)
                .refundAmount(refundAmount)
                .affectedRideGroupId(rideGroupId)
                .triggeredRebalance(rideGroupId != null)
                .build();

        // Update booking status
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Remove from ride group (triggers route re-optimization)
        if (booking.getRideGroup() != null) {
            rideGroupingService.removeBookingFromGroup(booking);
        }

        // Save cancellation
        cancellation = cancellationRepository.save(cancellation);

        // Publish cancellation event for async processing
        publishCancellationEvent(booking, rideGroupId, passengerId, request);

        log.info("Booking {} cancelled successfully. Fee: {}, Refund: {}",
                booking.getId(), cancellationFee, refundAmount);

        return cancellation;
    }


    private void validateCancellation(Booking booking) {
        if (!CANCELLABLE_STATUSES.contains(booking.getStatus())) {
            throw new CancellationException(
                    "Cannot cancel booking with status: " + booking.getStatus(),
                    "INVALID_STATUS_FOR_CANCELLATION"
            );
        }

        // Check if ride is too close to start
        if (booking.getStatus() == BookingStatus.CONFIRMED &&
            booking.getRideGroup() != null &&
            booking.getRideGroup().getActualDepartureTime() != null) {
            throw new CancellationException(
                    "Cannot cancel - ride has already started",
                    "RIDE_IN_PROGRESS"
            );
        }
    }

    private BigDecimal calculateCancellationFee(Booking booking) {
        if (booking.getFinalFare() == null) {
            return BigDecimal.ZERO;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pickupTime = booking.getRequestedPickupTime();

        long minutesUntilPickup = Duration.between(now, pickupTime).toMinutes();

        // Free cancellation if more than threshold minutes before pickup
        if (minutesUntilPickup > FREE_CANCELLATION_MINUTES) {
            return BigDecimal.ZERO;
        }

        // Apply cancellation fee
        return booking.getFinalFare().multiply(CANCELLATION_FEE_PERCENTAGE);
    }


    private BigDecimal calculateRefundAmount(Booking booking, BigDecimal cancellationFee) {
        if (booking.getFinalFare() == null) {
            return BigDecimal.ZERO;
        }
        return booking.getFinalFare().subtract(cancellationFee);
    }


    private void publishCancellationEvent(Booking booking, Long rideGroupId,
                                          Long passengerId, CancellationRequest request) {
        BookingCancelledEvent event = new BookingCancelledEvent(
                this,
                booking.getId(),
                rideGroupId,
                passengerId,
                request.getReason(),
                request.getInitiatedBy()
        );

        eventPublisher.publishEvent(event);
        log.debug("Published BookingCancelledEvent for booking: {}", booking.getId());
    }

    public boolean canCancel(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .map(booking -> CANCELLABLE_STATUSES.contains(booking.getStatus()))
                .orElse(false);
    }
}
