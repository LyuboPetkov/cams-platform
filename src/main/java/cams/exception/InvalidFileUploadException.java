package cams.exception;

public class InvalidFileUploadException extends RuntimeException {
    public InvalidFileUploadException(String message) {
        super(message);
    }
}
