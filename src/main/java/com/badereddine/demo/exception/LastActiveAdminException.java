package com.badereddine.demo.exception;

public class LastActiveAdminException extends RuntimeException {

    public static final String MESSAGE =
            "The last active administrator cannot be deleted, disabled, or demoted";

    public LastActiveAdminException() {
        super(MESSAGE);
    }
}
