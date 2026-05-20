package com.school.app.exceptions;

public class UserNotActiveInTheSystemException extends RuntimeException {
    public UserNotActiveInTheSystemException(String message) {
        super(message);
    }
}
