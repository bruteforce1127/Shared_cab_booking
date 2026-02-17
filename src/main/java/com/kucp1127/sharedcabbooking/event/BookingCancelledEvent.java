package com.kucp1127.sharedcabbooking.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;


@Getter
public class BookingCancelledEvent extends ApplicationEvent {

    private final Long bookingId;
    private final Long rideGroupId;
    private final Long passengerId;
    private final String reason;
    private final String initiatedBy;

    public BookingCancelledEvent(Object source, Long bookingId, Long rideGroupId,
                                  Long passengerId, String reason, String initiatedBy) {
        super(source);
        this.bookingId = bookingId;
        this.rideGroupId = rideGroupId;
        this.passengerId = passengerId;
        this.reason = reason;
        this.initiatedBy = initiatedBy;
    }
}
