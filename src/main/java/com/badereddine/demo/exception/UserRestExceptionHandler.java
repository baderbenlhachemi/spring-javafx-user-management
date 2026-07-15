package com.badereddine.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;

@ControllerAdvice
public class UserRestExceptionHandler {
    static final String VALIDATION_ERROR_MESSAGE = "Request validation failed";
    static final String MALFORMED_JSON_ERROR_MESSAGE = "Malformed JSON request";
    static final String NOT_FOUND_ERROR_MESSAGE = "User not found";
    static final String AUTHENTICATION_ERROR_MESSAGE = "Authentication failed";
    static final String AUTHORIZATION_ERROR_MESSAGE = "Access denied";
    static final String INTERNAL_ERROR_MESSAGE = "An internal error occurred";
    static final String BAD_REQUEST_ERROR_MESSAGE = "The request could not be processed";

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRestExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UserErrorResponse> handleValidationException(MethodArgumentNotValidException exc) {
        return errorResponse(HttpStatus.BAD_REQUEST, VALIDATION_ERROR_MESSAGE);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<UserErrorResponse> handleMalformedJsonException(HttpMessageNotReadableException exc) {
        return errorResponse(HttpStatus.BAD_REQUEST, MALFORMED_JSON_ERROR_MESSAGE);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<UserErrorResponse> handleUserNotFoundException(UserNotFoundException exc) {
        return errorResponse(HttpStatus.NOT_FOUND, NOT_FOUND_ERROR_MESSAGE);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<UserErrorResponse> handleInvalidPasswordException(InvalidPasswordException exc) {
        return errorResponse(HttpStatus.UNAUTHORIZED, AUTHENTICATION_ERROR_MESSAGE);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<UserErrorResponse> handleAccessDeniedException(AccessDeniedException exc) {
        return errorResponse(HttpStatus.FORBIDDEN, AUTHORIZATION_ERROR_MESSAGE);
    }

    @ExceptionHandler(LastActiveAdminException.class)
    public ResponseEntity<UserErrorResponse> handleLastActiveAdminException(LastActiveAdminException exc) {
        return errorResponse(HttpStatus.CONFLICT, LastActiveAdminException.MESSAGE);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<UserErrorResponse> handleIOException(IOException exc) {
        LOGGER.error("Internal request failure; exceptionType={}", exc.getClass().getName());
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MESSAGE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<UserErrorResponse> handleUnexpectedException(Exception exc) {
        LOGGER.error("Unexpected request failure; exceptionType={}", exc.getClass().getName());
        return errorResponse(HttpStatus.BAD_REQUEST, BAD_REQUEST_ERROR_MESSAGE);
    }

    private ResponseEntity<UserErrorResponse> errorResponse(HttpStatus status, String message) {
        UserErrorResponse error = new UserErrorResponse(status.value(), message, System.currentTimeMillis());
        return new ResponseEntity<>(error, status);
    }
}
