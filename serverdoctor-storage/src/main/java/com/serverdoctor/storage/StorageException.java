package com.serverdoctor.storage;

import com.serverdoctor.common.exception.ServerDoctorException;

import java.io.Serial;

/** Laufzeitfehler der Persistenzschicht. Unchecked, damit Repos ergonomisch bleiben. */
public class StorageException extends ServerDoctorException {

    @Serial
    private static final long serialVersionUID = 1L;

    public StorageException(String message) { super(message); }
    public StorageException(String message, Throwable cause) { super(message, cause); }
}
