package com.kucp1127.sharedcabbooking.exception;


public class RideGroupException extends SharedCabException {

    public RideGroupException(String message) {
        super(message, "RIDE_GROUP_ERROR");
    }

    public RideGroupException(String message, String errorCode) {
        super(message, errorCode);
    }
}
