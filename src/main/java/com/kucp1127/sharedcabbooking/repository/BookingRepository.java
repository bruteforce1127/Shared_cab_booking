package com.kucp1127.sharedcabbooking.repository;

import com.kucp1127.sharedcabbooking.domain.entity.Booking;
import com.kucp1127.sharedcabbooking.domain.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByPassengerId(Long passengerId);

    List<Booking> findByPassengerIdAndStatus(Long passengerId, BookingStatus status);

    Page<Booking> findByPassengerIdOrderByCreatedAtDesc(Long passengerId, Pageable pageable);

    List<Booking> findByRideGroupId(Long rideGroupId);

    List<Booking> findByStatus(BookingStatus status);

    /**
     * Find pending bookings within time window for matching.
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.status = 'PENDING' 
        AND b.requestedPickupTime BETWEEN :start AND :end
        ORDER BY b.requestedPickupTime
        """)
    List<Booking> findPendingBookingsInTimeWindow(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Find pending bookings near a location for pooling.
     */
    @Query(value = """
        SELECT b.* FROM bookings b 
        WHERE b.status = 'PENDING'
        AND b.ride_group_id IS NULL
        AND b.requested_pickup_time BETWEEN :timeStart AND :timeEnd
        AND b.pickup_latitude IS NOT NULL 
        AND b.pickup_longitude IS NOT NULL
        AND (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(b.pickup_latitude)) * 
                cos(radians(b.pickup_longitude) - radians(:lng)) + 
                sin(radians(:lat)) * sin(radians(b.pickup_latitude))
            )
        ) <= :radiusKm
        ORDER BY b.requested_pickup_time
        LIMIT :limit
        """, nativeQuery = true)
    List<Booking> findNearbyPendingBookings(
            @Param("lat") Double latitude,
            @Param("lng") Double longitude,
            @Param("radiusKm") Double radiusKm,
            @Param("timeStart") LocalDateTime timeStart,
            @Param("timeEnd") LocalDateTime timeEnd,
            @Param("limit") Integer limit
    );

    /**
     * Find and lock booking for update.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithLock(@Param("id") Long id);

    /**
     * Count active bookings (for surge pricing).
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS')")
    long countActiveBookings();

    /**
     * Count pending bookings in time window (for demand calculation).
     */
    @Query("""
        SELECT COUNT(b) FROM Booking b 
        WHERE b.status = 'PENDING' 
        AND b.requestedPickupTime BETWEEN :start AND :end
        """)
    long countPendingInTimeWindow(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Find expired pending bookings for cleanup.
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.status = 'PENDING' 
        AND b.createdAt < :threshold
        """)
    List<Booking> findExpiredPendingBookings(@Param("threshold") LocalDateTime threshold);
}
