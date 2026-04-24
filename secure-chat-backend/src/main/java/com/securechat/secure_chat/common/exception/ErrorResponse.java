package com.securechat.secure_chat.common.exception;

import lombok.Getter;

import java.time.Instant;

@Getter
public class ErrorResponse {

    private final int status;
    private final String message;
    private final Instant timestamp;

    public ErrorResponse(int status, String message) {
        this.status    = status;
        this.message   = message;
        this.timestamp = Instant.now();
    }
}