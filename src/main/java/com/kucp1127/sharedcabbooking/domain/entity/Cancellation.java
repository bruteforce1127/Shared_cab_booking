package com.kucp1127.sharedcabbooking.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancellations", indexes = {
    @Index(name = "idx_cancellation_booking", columnList = "booking_id"),
    @Index(name = "idx_cancellation_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cancellation extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "cancelled_at", nullable = false)
    private LocalDateTime cancelledAt;

    @Column(name = "reason")
    private String reason;

    /**
     * Whether cancellation was initiated by passenger or system.
     */
    @Column(name = "initiated_by", nullable = false)
    private String initiatedBy;

    /**
     * Cancellation fee charged (if any).
     */
    @Column(name = "cancellation_fee", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal cancellationFee = BigDecimal.ZERO;

    /**
     * Refund amount processed.
     */
    @Column(name = "refund_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

    /**
     * Whether this cancellation triggered a group rebalance.
     */
    @Column(name = "triggered_rebalance")
    @Builder.Default
    private Boolean triggeredRebalance = false;

    /**
     * ID of the ride group affected (captured for audit).
     */
    @Column(name = "affected_ride_group_id")
    private Long affectedRideGroupId;

    @PrePersist
    public void setDefaults() {
        if (cancelledAt == null) {
            cancelledAt = LocalDateTime.now();
        }
    }
}
