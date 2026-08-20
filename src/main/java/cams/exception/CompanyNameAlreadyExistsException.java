package cams.exception;

public class CompanyNameAlreadyExistsException extends RuntimeException {
    public CompanyNameAlreadyExistsException(String message) {
        super(message);
    }
}
