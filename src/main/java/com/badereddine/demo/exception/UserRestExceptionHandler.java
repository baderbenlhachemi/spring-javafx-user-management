package com.badereddine.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;

@ControllerAdvice
public class UserRestExceptionHandler {
    @ExceptionHandler
    public ResponseEntity<UserErrorResponse> handleException(UserNotFoundException exc) {
        UserErrorResponse error = new UserErrorResponse();

        error.setStatus(404);
        error.setMessage(exc.getMessage());
        error.setTimeStamp(System.currentTimeMillis());

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<UserErrorResponse> handleException(InvalidPasswordException exc) {
        UserErrorResponse error = new UserErrorResponse();

        error.setStatus(401);
        error.setMessage(exc.getMessage());
        error.setTimeStamp(System.currentTimeMillis());

        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler
    public ResponseEntity<UserErrorResponse> handleException(AccessDeniedException exc) {
        UserErrorResponse error = new UserErrorResponse();

        error.setStatus(403);
        error.setMessage(exc.getMessage());
        error.setTimeStamp(System.currentTimeMillis());

        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler
    public ResponseEntity<UserErrorResponse> handleException(LastActiveAdminException exc) {
        UserErrorResponse error = new UserErrorResponse();

        error.setStatus(HttpStatus.CONFLICT.value());
        error.setMessage(exc.getMessage());
        error.setTimeStamp(System.currentTimeMillis());

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler
    public ResponseEntity<UserErrorResponse> handleException(IOException exc) {
        UserErrorResponse error = new UserErrorResponse();

        error.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.setMessage(exc.getMessage());
        error.setTimeStamp(System.currentTimeMillis());

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler
    public ResponseEntity<UserErrorResponse> handleException(Exception exc) {
        UserErrorResponse error = new UserErrorResponse();

        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setMessage(exc.getMessage());
        error.setTimeStamp(System.currentTimeMillis());

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
