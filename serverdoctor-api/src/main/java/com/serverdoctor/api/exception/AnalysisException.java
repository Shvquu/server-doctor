package com.serverdoctor.api.exception;

import com.serverdoctor.common.exception.ServerDoctorException;

import java.io.Serial;

/** A failure while running the analysis or inside a scanner module. */
public class AnalysisException extends ServerDoctorException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AnalysisException(String message) { super(message); }
    public AnalysisException(String message, Throwable cause) { super(message, cause); }
}

