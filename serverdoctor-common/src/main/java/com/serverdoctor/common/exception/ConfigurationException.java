package com.serverdoctor.common.exception;

import java.io.Serial;

/** Invalid or unreadable configuration / settings (e.g. an unknown storage type). */
public class ConfigurationException extends ServerDoctorException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConfigurationException(String message) { super(message); }
    public ConfigurationException(String message, Throwable cause) { super(message, cause); }
}
