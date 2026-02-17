package com.kucp1127.sharedcabbooking.repository;

import com.kucp1127.sharedcabbooking.domain.entity.RideGroup;
import com.kucp1127.sharedcabbooking.domain.enums.RideGroupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RideGroup entity with optimized pooling queries.
 */
@Repository
public interface RideGroupRepository extends JpaRepository<RideGroup, Long> {

    List<RideGroup> findByStatus(RideGroupStatus status);

    List<RideGroup> findByStatusAndScheduledDepartureTimeBetween(
            RideGroupStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * Find forming ride groups near a pickup location within time window.
     * Core query for ride matching algorithm.
     */
    @Query(value = """
        SELECT rg.* FROM ride_groups rg
        JOIN bookings b ON b.ride_group_id = rg.id
        WHERE rg.status = 'FORMING'
        AND rg.scheduled_departure_time BETWEEN :timeStart AND :timeEnd
        AND rg.total_passengers < (
            SELECT CASE c.cab_type 
                WHEN 'SEDAN' THEN 4 
                WHEN 'SUV' THEN 6 
                WHEN 'VAN' THEN 8 
                WHEN 'PREMIUM_SEDAN' THEN 4 
            END
            FROM cabs c WHERE c.id = rg.cab_id
        )
        AND EXISTS (
            SELECT 1 FROM bookings b2 
            WHERE b2.ride_group_id = rg.id 
            AND (
                6371 * acos(
                    cos(radians(:lat)) * cos(radians(b2.pickup_latitude)) * 
                    cos(radians(b2.pickup_longitude) - radians(:lng)) + 
                    sin(radians(:lat)) * sin(radians(b2.pickup_latitude))
                )
            ) <= :proximityRadiusKm
        )
        GROUP BY rg.id
        ORDER BY rg.scheduled_departure_time
        LIMIT :limit
        """, nativeQuery = true)
    List<RideGroup> findMatchingRideGroups(
            @Param("lat") Double pickupLatitude,
            @Param("lng") Double pickupLongitude,
            @Param("proximityRadiusKm") Double proximityRadiusKm,
            @Param("timeStart") LocalDateTime timeStart,
            @Param("timeEnd") LocalDateTime timeEnd,
            @Param("limit") Integer limit
    );

    /**
     * Find and lock a ride group for update (pessimistic locking).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rg FROM RideGroup rg WHERE rg.id = :id")
    Optional<RideGroup> findByIdWithLock(@Param("id") Long id);

    /**
     * Find ride groups by cab.
     */
    List<RideGroup> findByCabIdAndStatusIn(Long cabId, List<RideGroupStatus> statuses);

    /**
     * Count active ride groups.
     */
    @Query("SELECT COUNT(rg) FROM RideGroup rg WHERE rg.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<RideGroupStatus> statuses);

    /**
     * Find ride groups that need dispatch (scheduled time approaching).
     */
    @Query("""
        SELECT rg FROM RideGroup rg 
        WHERE rg.status = 'FORMING' 
        AND rg.scheduledDepartureTime <= :threshold
        ORDER BY rg.scheduledDepartureTime
        """)
    List<RideGroup> findGroupsReadyForDispatch(@Param("threshold") LocalDateTime threshold);
}
