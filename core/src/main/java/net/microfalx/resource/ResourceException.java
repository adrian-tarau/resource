package net.microfalx.resource;

/**
 * An exception for various resource failures, not related to I/O.
 */
public class ResourceException extends RuntimeException {

    public ResourceException(String message) {
        super(message);
    }

    public ResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
