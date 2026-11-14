package com.fleetcontrol.exception;

import org.springframework.http.HttpStatus;

/** Error de negocio con código HTTP. El mensaje se muestra al usuario. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
