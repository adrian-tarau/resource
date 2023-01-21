package net.microfalx.resource;

import java.io.IOException;

/**
 * An interface using to walk resources.
 */
@FunctionalInterface
public interface ResourceVisitor {

    /**
     * Invoked for each child resource, at any depth.
     *
     * @param root  the root (starting) resource
     * @param child the child resource
     * @return <code>true</code> to continue the walk
     * @throws IOException if an I/O error occurs
     */
    boolean onResource(Resource root, Resource child) throws IOException;
}
