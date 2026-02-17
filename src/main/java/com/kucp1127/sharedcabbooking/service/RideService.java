package com.kucp1127.sharedcabbooking.service;

import com.kucp1127.sharedcabbooking.domain.entity.Booking;
import com.kucp1127.sharedcabbooking.domain.entity.RideGroup;
import com.kucp1127.sharedcabbooking.domain.enums.BookingStatus;
import com.kucp1127.sharedcabbooking.dto.mapper.EntityDtoMapper;
import com.kucp1127.sharedcabbooking.dto.request.RideRequest;
import com.kucp1127.sharedcabbooking.dto.response.RideGroupResponse;
import com.kucp1127.sharedcabbooking.dto.response.RideResponse;
import com.kucp1127.sharedcabbooking.exception.ResourceNotFoundException;
import com.kucp1127.sharedcabbooking.repository.BookingRepository;
import com.kucp1127.sharedcabbooking.repository.RideGroupRepository;
import com.kucp1127.sharedcabbooking.service.matching.RideGroupingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RideService {

    private final RideGroupingService rideGroupingService;
    private final BookingRepository bookingRepository;
    private final RideGroupRepository rideGroupRepository;
    private final EntityDtoMapper mapper;

    /**
     * Book a new ride.
     */
    @Transactional
    public RideResponse bookRide(RideRequest request) {
        log.info("Processing ride booking for passenger: {}", request.getPassengerId());

        Booking booking = rideGroupingService.processRideRequest(request);

        log.info("Ride booked successfully. Booking ID: {}, Group ID: {}",
                booking.getId(),
                booking.getRideGroup() != null ? booking.getRideGroup().getId() : "N/A");

        return mapper.toRideResponse(booking);
    }

    /**
     * Get ride/booking by ID.
     */
    @Transactional(readOnly = true)
    public RideResponse getRide(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        return mapper.toRideResponse(booking);
    }

    /**
     * Get all rides for a passenger.
     */
    @Transactional(readOnly = true)
    public List<RideResponse> getPassengerRides(Long passengerId) {
        return bookingRepository.findByPassengerId(passengerId).stream()
                .map(mapper::toRideResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get paginated rides for a passenger.
     */
    @Transactional(readOnly = true)
    public Page<RideResponse> getPassengerRidesPaginated(Long passengerId, Pageable pageable) {
        return bookingRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId, pageable)
                .map(mapper::toRideResponse);
    }

    /**
     * Get active rides for a passenger.
     */
    @Transactional(readOnly = true)
    public List<RideResponse> getActiveRides(Long passengerId) {
        List<BookingStatus> activeStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.CONFIRMED,
                BookingStatus.IN_PROGRESS
        );

        return bookingRepository.findByPassengerId(passengerId).stream()
                .filter(b -> activeStatuses.contains(b.getStatus()))
                .map(mapper::toRideResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get ride group details.
     */
    @Transactional(readOnly = true)
    public RideGroupResponse getRideGroup(Long groupId) {
        RideGroup group = rideGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("RideGroup", groupId));
        return mapper.toRideGroupResponse(group);
    }

    /**
     * Get ride group for a booking.
     */
    @Transactional(readOnly = true)
    public RideGroupResponse getRideGroupForBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (booking.getRideGroup() == null) {
            throw new ResourceNotFoundException("RideGroup for booking", bookingId);
        }

        return mapper.toRideGroupResponse(booking.getRideGroup());
    }
}
