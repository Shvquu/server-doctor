package com.serverdoctor.storage;

/** Laufzeitfehler der Persistenzschicht. Unchecked, damit Repos ergonomisch bleiben. */
public class StorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StorageException(String message) { super(message); }
    public StorageException(String message, Throwable cause) { super(message, cause); }
}
