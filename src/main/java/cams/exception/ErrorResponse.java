package cams.exception;

import java.time.LocalDateTime;

public record ErrorResponse(int status,
                            Object message,
                            LocalDateTime timestamp
) {
    public ErrorResponse(int status, Object message) {
        this(status, message, LocalDateTime.now());
    }
}
