package com.kucp1127.sharedcabbooking.exception;


public class NoCabAvailableException extends SharedCabException {

    public NoCabAvailableException() {
        super("No cabs available at the moment", "NO_CAB_AVAILABLE");
    }

    public NoCabAvailableException(String message) {
        super(message, "NO_CAB_AVAILABLE");
    }
}
