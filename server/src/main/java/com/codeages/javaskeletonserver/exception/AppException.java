package com.codeages.javaskeletonserver.exception;

import com.codeages.javaskeletonserver.common.ErrorCodeInterface;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.stream.Collectors;

public class AppException extends RuntimeException {

    private final ErrorCodeInterface error;

    public AppException(ErrorCodeInterface error) {
        this(error, null, null);
    }

    public AppException(ErrorCodeInterface error, String message) {
        this(error, message, null);
    }

    public AppException(ErrorCodeInterface error, Throwable cause) {
        this(error, null, cause);
    }

    public AppException(ErrorCodeInterface error, String message, Throwable cause) {
        super(message == null ? error.getMessage() : message, cause);
        this.error = error;
    }

    public AppException(ErrorCodeInterface error, Set<? extends ConstraintViolation<?>> constraintViolations) {
        this(error, convertViolationsToString(constraintViolations));
    }

    public String getCode() {
        return error.getCode();
    }

    public int getStatus() {
        return error.getStatus();
    }

    private static String convertViolationsToString(Set<? extends ConstraintViolation<?>> constraintViolations) {
        if (constraintViolations == null) {
            return null;
        }

        return constraintViolations.stream()
                .map( cv -> cv == null ? "null" : cv.getPropertyPath() + ": " + cv.getMessage() )
                .collect( Collectors.joining( ", " ) );
    }
}
