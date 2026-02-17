package com.kucp1127.sharedcabbooking.repository;

import com.kucp1127.sharedcabbooking.domain.entity.Cancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Cancellation entity.
 */
@Repository
public interface CancellationRepository extends JpaRepository<Cancellation, Long> {

    Optional<Cancellation> findByBookingId(Long bookingId);

    List<Cancellation> findByAffectedRideGroupId(Long rideGroupId);

    @Query("""
        SELECT c FROM Cancellation c 
        WHERE c.cancelledAt BETWEEN :start AND :end 
        ORDER BY c.cancelledAt DESC
        """)
    List<Cancellation> findCancellationsInPeriod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Count cancellations by initiator type.
     */
    @Query("SELECT COUNT(c) FROM Cancellation c WHERE c.initiatedBy = :initiatedBy")
    long countByInitiatedBy(@Param("initiatedBy") String initiatedBy);
}
