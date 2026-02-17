package com.kucp1127.sharedcabbooking.exception;


public class CancellationException extends SharedCabException {

    public CancellationException(String message) {
        super(message, "CANCELLATION_ERROR");
    }

    public CancellationException(String message, String errorCode) {
        super(message, errorCode);
    }
}
