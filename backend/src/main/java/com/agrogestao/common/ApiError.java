package com.agrogestao.common;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiError {

    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;

    public static ApiError of(int status, String error, String message) {
        return new ApiError(LocalDateTime.now(), status, error, message);
    }
}
