package com.serverdoctor.common.exception;

import java.io.Serial;

public class ServerDoctorException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ServerDoctorException(String message) {
        super(message);
    }

    public ServerDoctorException(String message, Throwable cause) {
        super(message, cause);
    }

}
