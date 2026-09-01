package com.nia.common;

import org.springframework.http.HttpStatus;

/**
 * Application exception carrying a stable snake_case code, an HTTP status, and a
 * user-safe message. The global handler turns it into an {@link ApiError}.
 */
public class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }

    // ---- Common factory helpers (messages come from NIA's error table) ------

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "not_found", message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "invalid_input", message);
    }

    public static ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Please sign in to continue.");
    }

    public static ApiException rateLimited(String message) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "rate_limited", message);
    }

    public static ApiException upstreamUnavailable(String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "upstream_unavailable", message);
    }
}
