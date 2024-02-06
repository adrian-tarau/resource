package net.microfalx.resource;

import java.util.Collection;

/**
 * A resource which is made out of multiple resource.
 */
public interface CompositeResource extends Resource {

    /**
     * Returns a collection with resources part of this resource.
     *
     * @return a non-null instance
     */
    Collection<Resource> getResources();
}
