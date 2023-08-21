package net.microfalx.resource;

import java.io.InputStream;

/**
 * A resource processor which can change the resource (content).
 */
public interface ResourceProcessor {

    /**
     * Returns a different stream.
     *
     * @param resource    the resource.
     * @param inputStream the input stream
     * @return the same input stream, a new stream if the processor can intercept the resource
     */
    default InputStream getInputStream(Resource resource, InputStream inputStream) {
        return inputStream;
    }
}
