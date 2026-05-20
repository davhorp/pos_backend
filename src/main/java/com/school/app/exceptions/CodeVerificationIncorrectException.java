package com.school.app.exceptions;

public class CodeVerificationIncorrectException extends RuntimeException {
    public CodeVerificationIncorrectException(String message) {
        super(message);
    }
}
