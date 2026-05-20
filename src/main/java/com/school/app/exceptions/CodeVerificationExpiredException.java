package com.school.app.exceptions;

public class CodeVerificationExpiredException extends RuntimeException {
    public CodeVerificationExpiredException(String message) {
        super(message);
    }
}
