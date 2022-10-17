package com.codeages.javaskeletonserver.exception;

import com.codeages.javaskeletonserver.biz.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({AppException.class})
    public final ResponseEntity<AppError> handleAppExceptions(AppException e, HttpServletRequest request) {
        return ResponseEntity.status(e.getStatus())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(AppError.fromAppException(e, request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public final ResponseEntity<AppError> handleValidationFailedException(ConstraintViolationException e, HttpServletRequest request) {
        return handleAppExceptions(new AppException(ErrorCode.INVALID_ARGUMENT, e.getMessage()), request);
    }
}