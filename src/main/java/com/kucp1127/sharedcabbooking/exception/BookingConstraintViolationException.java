package com.kucp1127.sharedcabbooking.exception;


public class BookingConstraintViolationException extends SharedCabException {

    public BookingConstraintViolationException(String message) {
        super(message, "CONSTRAINT_VIOLATION");
    }

    public BookingConstraintViolationException(String message, String errorCode) {
        super(message, errorCode);
    }
}
