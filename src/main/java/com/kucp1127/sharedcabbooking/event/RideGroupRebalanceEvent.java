package com.kucp1127.sharedcabbooking.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RideGroupRebalanceEvent extends ApplicationEvent {

    private final Long rideGroupId;
    private final String reason;

    public RideGroupRebalanceEvent(Object source, Long rideGroupId, String reason) {
        super(source);
        this.rideGroupId = rideGroupId;
        this.reason = reason;
    }
}
