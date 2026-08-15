package com.lyubo_learning.cams.exception;

public class JobListingAlreadyArchivedException extends RuntimeException {
    public JobListingAlreadyArchivedException(String message) {
        super(message);
    }
}
