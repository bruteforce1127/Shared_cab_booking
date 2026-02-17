package com.kucp1127.sharedcabbooking.repository;

import com.kucp1127.sharedcabbooking.domain.entity.Cab;
import com.kucp1127.sharedcabbooking.domain.enums.CabStatus;
import com.kucp1127.sharedcabbooking.domain.enums.CabType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Cab entity with custom geo-proximity queries.
 */
@Repository
public interface CabRepository extends JpaRepository<Cab, Long> {

    Optional<Cab> findByLicensePlate(String licensePlate);

    List<Cab> findByStatus(CabStatus status);

    List<Cab> findByCabTypeAndStatus(CabType cabType, CabStatus status);

    /**
     * Find available cabs within a radius using Haversine formula.
     * Returns cabs sorted by distance.
     */
    @Query(value = """
        SELECT c.* FROM cabs c 
        WHERE c.status = 'AVAILABLE' 
        AND c.current_latitude IS NOT NULL 
        AND c.current_longitude IS NOT NULL
        AND (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(c.current_latitude)) * 
                cos(radians(c.current_longitude) - radians(:lng)) + 
                sin(radians(:lat)) * sin(radians(c.current_latitude))
            )
        ) <= :radiusKm
        ORDER BY (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(c.current_latitude)) * 
                cos(radians(c.current_longitude) - radians(:lng)) + 
                sin(radians(:lat)) * sin(radians(c.current_latitude))
            )
        )
        LIMIT :limit
        """, nativeQuery = true)
    List<Cab> findAvailableCabsNearLocation(
            @Param("lat") Double latitude,
            @Param("lng") Double longitude,
            @Param("radiusKm") Double radiusKm,
            @Param("limit") Integer limit
    );

    /**
     * Find available cabs of specific type within radius.
     */
    @Query(value = """
        SELECT c.* FROM cabs c 
        WHERE c.status = 'AVAILABLE' 
        AND c.cab_type = :cabType
        AND c.current_latitude IS NOT NULL 
        AND c.current_longitude IS NOT NULL
        AND c.available_seats >= :requiredSeats
        AND c.available_luggage_capacity_kg >= :requiredLuggageKg
        AND (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(c.current_latitude)) * 
                cos(radians(c.current_longitude) - radians(:lng)) + 
                sin(radians(:lat)) * sin(radians(c.current_latitude))
            )
        ) <= :radiusKm
        ORDER BY (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(c.current_latitude)) * 
                cos(radians(c.current_longitude) - radians(:lng)) + 
                sin(radians(:lat)) * sin(radians(c.current_latitude))
            )
        )
        LIMIT :limit
        """, nativeQuery = true)
    List<Cab> findSuitableCabsNearLocation(
            @Param("lat") Double latitude,
            @Param("lng") Double longitude,
            @Param("radiusKm") Double radiusKm,
            @Param("cabType") String cabType,
            @Param("requiredSeats") Integer requiredSeats,
            @Param("requiredLuggageKg") Double requiredLuggageKg,
            @Param("limit") Integer limit
    );

    /**
     * Count available cabs by type.
     */
    @Query("SELECT COUNT(c) FROM Cab c WHERE c.status = :status AND c.cabType = :cabType")
    long countByStatusAndCabType(@Param("status") CabStatus status, @Param("cabType") CabType cabType);
}
