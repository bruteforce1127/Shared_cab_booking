package com.kucp1127.sharedcabbooking.domain.enums;

public enum RideGroupStatus {
    FORMING,        // Group is being formed, accepting new passengers
    LOCKED,         // Group is locked, no new passengers can join
    DISPATCHED,     // Cab has been dispatched for pickup
    IN_PROGRESS,    // Ride is in progress
    COMPLETED,      // All passengers dropped off
    CANCELLED       // Group cancelled (all passengers cancelled)
}
