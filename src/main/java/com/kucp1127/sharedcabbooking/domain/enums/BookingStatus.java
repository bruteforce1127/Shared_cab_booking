package com.kucp1127.sharedcabbooking.domain.enums;


public enum BookingStatus {
    PENDING,        // Initial state when booking is created
    CONFIRMED,      // Booking confirmed and assigned to a ride group
    IN_PROGRESS,    // Ride is currently active
    COMPLETED,      // Ride successfully completed
    CANCELLED,      // Cancelled by passenger
    EXPIRED         // Booking expired due to timeout
}
