package net.tarau.resource;

import java.net.URI;

/**
 * A factory used to create resources.
 */
public class ResourceFactory {

    /**
     * Creates a resource from an URI.
     *
     * @param uri the URI
     * @return the resource
     */
    public static Resource resolve(URI uri) {
        ResourceUtils.requireNonNull(uri);
        return null;
    }
}
