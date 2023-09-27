package net.microfalx.resource;

import java.net.URI;

/**
 * An interface used to provide plug'n'play mechanism for resolving resources.
 */
public interface ResourceResolver {

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
     * @param type the request type for resource, can be NULL for auto-detection
     * @return the resource
     */
    Resource resolve(URI uri, Resource.Type type);

    /**
     * Clears any caches use by provider.
     */
    default void clearCache() {
        // empty on purpose
    }
}
