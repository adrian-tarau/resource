package net.microfalx.resource;

import java.net.URI;

/**
 * An interface used to provide plug'n'play mechanism for resolving resources.
 */
public interface ResourceResolver {

    int LOW_ORDER = 0;
    int DEFAULT_PRIORITY = 100;
    int HIGH_PRIORITY = 200;

    /**
     * Returns whether the resolve supports the scheme and can resolve resources.
     *
     * @param uri the URI representing the resource
     * @return {@code true} if the
     */
    boolean supports(URI uri);

    /**
     * Returns a resource for a given URI.
     *
     * @param uri the URI for the resource
     * @return the resource
     */
    Resource resolve(URI uri);

    /**
     * Returns the order in which the resolver is called.
     * @return an interger
     */
    default int getOrder() {
        return DEFAULT_PRIORITY;
    }
}
