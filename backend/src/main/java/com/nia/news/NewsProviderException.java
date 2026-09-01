package com.nia.news;

/** Raised when a provider call fails (network, quota, bad response). */
public class NewsProviderException extends Exception {
    public NewsProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public NewsProviderException(String message) {
        super(message);
    }
}
