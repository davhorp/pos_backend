package com.school.app.exceptions;

public class UserBadCredentialsException extends RuntimeException {
    public UserBadCredentialsException(String message) {
        super(message);
    }
}
