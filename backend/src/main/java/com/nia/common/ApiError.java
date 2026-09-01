package com.nia.common;

/**
 * The single, consistent error shape returned by every endpoint:
 * {@code { "error": "code_snake_case", "message": "Human-readable message" }}.
 * Never contains stack traces, SQL, URLs, or provider details.
 */
public record ApiError(String error, String message) {
}
