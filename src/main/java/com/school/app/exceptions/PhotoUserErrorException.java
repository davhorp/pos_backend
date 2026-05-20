package com.school.app.exceptions;

public class PhotoUserErrorException extends RuntimeException {
    public PhotoUserErrorException(String message) {
        super(message);
    }
}
