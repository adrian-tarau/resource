package net.microfalx.resource;

import java.io.IOException;

/**
 * An interface using to walk resources.
 */
@FunctionalInterface
public interface ResourceVisitor {

    /**
     * invoked for each child resource.
     *
     * @param parent the parent resource
     * @param child  the child resource
     * @param depth  the depth
     * @return <code>true</code> to continue the walk
     * @throws IOException
     */
    boolean onResource(Resource parent, Resource child, int depth) throws IOException;
}
