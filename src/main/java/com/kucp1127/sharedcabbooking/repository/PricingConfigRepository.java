package com.kucp1127.sharedcabbooking.repository;

import com.kucp1127.sharedcabbooking.domain.entity.PricingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PricingConfig entity.
 * Supports Chain of Responsibility pattern for pricing.
 */
@Repository
public interface PricingConfigRepository extends JpaRepository<PricingConfig, Long> {

    List<PricingConfig> findByConfigTypeAndIsActiveTrueOrderByPriority(String configType);

    Optional<PricingConfig> findByConfigTypeAndConfigKeyAndIsActiveTrue(String configType, String configKey);

    @Query("""
        SELECT pc FROM PricingConfig pc 
        WHERE pc.isActive = true 
        ORDER BY pc.configType, pc.priority
        """)
    List<PricingConfig> findAllActiveConfigs();

    /**
     * Get numeric value for a specific config.
     */
    @Query("""
        SELECT pc.numericValue FROM PricingConfig pc 
        WHERE pc.configType = :type 
        AND pc.configKey = :key 
        AND pc.isActive = true
        """)
    Optional<java.math.BigDecimal> getNumericValue(
            @Param("type") String configType,
            @Param("key") String configKey
    );
}
