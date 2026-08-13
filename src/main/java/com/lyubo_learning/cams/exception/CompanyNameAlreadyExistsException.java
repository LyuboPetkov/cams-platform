package com.lyubo_learning.cams.exception;

public class CompanyNameAlreadyExistsException extends RuntimeException {
    public CompanyNameAlreadyExistsException(String message) {
        super(message);
    }
}
