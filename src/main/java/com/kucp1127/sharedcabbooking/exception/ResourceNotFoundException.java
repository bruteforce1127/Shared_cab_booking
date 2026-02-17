package com.kucp1127.sharedcabbooking.exception;


public class ResourceNotFoundException extends SharedCabException {

    public ResourceNotFoundException(String resourceType, Long id) {
        super(resourceType + " not found with id: " + id, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " not found: " + identifier, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }
}
