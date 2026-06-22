package com.serverdoctor.api.exception;

import com.serverdoctor.common.exception.ServerDoctorException;

import java.io.Serial;

/** Thrown when the public API is used before the plugin has registered it. */
public class ApiNotInitializedException extends ServerDoctorException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ApiNotInitializedException(String message) { super(message); }
}
