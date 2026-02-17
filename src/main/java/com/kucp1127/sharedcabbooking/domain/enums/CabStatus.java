package com.kucp1127.sharedcabbooking.domain.enums;


public enum CabStatus {
    AVAILABLE,      // Cab is available for new rides
    ASSIGNED,       // Cab assigned to a ride group but not yet dispatched
    EN_ROUTE,       // Cab is en route to pickup
    ON_TRIP,        // Cab is on an active trip
    OFFLINE         // Cab is offline/unavailable
}
