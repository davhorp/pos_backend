package com.school.app.dto.response;

import java.util.Date;

public record ExceptionResponse(
        Date timestamp,
        int status,
        String error,
        String message,
        String typeException
) {
}
