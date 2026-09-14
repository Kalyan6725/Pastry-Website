package org.example.backend.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

public record ApiError(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime timestamp,
        int status,
        String message,
        String path
) {
}
