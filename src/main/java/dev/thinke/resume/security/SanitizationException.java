package dev.thinke.resume.security;

public class SanitizationException extends RuntimeException {

    public SanitizationException(String message) {
        super(message);
    }
}
