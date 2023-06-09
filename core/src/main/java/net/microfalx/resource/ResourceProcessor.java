package net.microfalx.resource;

import java.io.InputStream;

/**
 * A resource processor which can change the resource (content).
 */
public interface ResourceProcessor {

    int LOW_ORDER = 0;
    int DEFAULT_PRIORITY = 100;
    int HIGH_PRIORITY = 200;

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

    /**
     * Returns the order in which the resolver is called.
     * @return an interger
     */
    default int getOrder() {
        return DEFAULT_PRIORITY;
    }
}
