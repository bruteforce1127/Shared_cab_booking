package com.kucp1127.sharedcabbooking.exception;


public class SharedCabException extends RuntimeException {

    private final String errorCode;

    public SharedCabException(String message) {
        super(message);
        this.errorCode = "GENERAL_ERROR";
    }

    public SharedCabException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public SharedCabException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
