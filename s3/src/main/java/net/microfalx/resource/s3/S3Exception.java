package net.microfalx.resource.s3;

import net.microfalx.resource.ResourceException;

/**
 * An exception for any S3 issue.
 */
public class S3Exception extends ResourceException {

    public S3Exception(String message) {
        super(message);
    }

    public S3Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
