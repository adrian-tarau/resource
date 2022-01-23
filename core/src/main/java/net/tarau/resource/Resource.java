package net.tarau.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Collection;

/**
 * An interface for a resource descriptor that abstracts from the actual
 * type of underlying resource, such as a file or class path resource.
 */
public interface Resource extends Serializable {

    /**
     * Returns the credential used to access the resource.
     *
     * @return a non-null instance
     */
    Credential getCredential();

    /**
     * Returns a unique identifier for this resource.
     *
     * @return a non-null instance
     */
    String getId();

    /**
     * Returns the (friendly) name for a resource (usually the file name, unless it was changed).
     *
     * @return a non-null instance
     */
    String getName();

    /**
     * Returns a description associated with this resource.
     * <p>
     * Resources usually do not have descriptions, but various services can attach descriptions to resources.
     *
     * @return the description, null if not available
     */
    String getDescription();

    /**
     * Returns the resource type.
     *
     * @return a non-null enum
     */
    Type getType();

    /**
     * Returns the resource's parent.
     *
     * @return a non-null instance if the resource has a parent, null otherwise
     */
    Resource getParent();

    /**
     * Returns the filename of this resource (including the extension), without the path.
     *
     * @return a non-empty string
     */
    String getFileName();

    /**
     * Returns the file extension of this resource.
     *
     * @return a non-null string
     */
    String getFileExtension();

    /**
     * Returns the relative path of the resource to another resource.
     *
     * @param resource the resource
     * @return the relative path
     */
    String getPath(Resource resource);

    /**
     * Returns the resource path relative to the <code>root</code>.
     * <p>
     * The path will always start with "/".
     *
     * @return the path
     */
    String getPath();

    /**
     * Loads this resource as a string.
     *
     * @return The string contents of the resource.
     */
    String loadAsString() throws IOException;

    /**
     * Loads this resource as a byte array.
     *
     * @return The contents of the resource.
     */
    byte[] loadAsBytes() throws IOException;

    /**
     * Returns the resource input stream.
     *
     * @return a non-null stream
     * @throws IOException if an I/O error occurs
     */
    InputStream getInputStream() throws IOException;

    /**
     * Returns whether the resource can be read.
     *
     * @see #getInputStream()
     * @see #exists()
     */
    default boolean isReadable() {
        return getType() == Type.FILE && exists();
    }

    /**
     * Returns whether this resource exists.
     *
     * @return {@code true} if it exists, {@code false} if not.
     */
    boolean exists();

    /**
     * Returns the last modified timestamp for the resource.
     *
     * @return a positive integer, a negative integer if unknown
     */
    long lastModified();

    /**
     * Returns the size of the resource.
     *
     * @return a positive integer, a negative integer if unknown
     */
    long length();

    /**
     * Returns the content type (mimetype) based on the file name extension.
     *
     * @return the content type
     */
    String getContentType();

    /**
     * Returns an attribute associated with the resource.
     *
     * @param name the name
     * @return the value, null if it does not exist
     */
    <T> T getAttribute(String name);

    /**
     * Lists child resources.
     *
     * @return a non-null instance
     */
    Collection<Resource> list();

    /**
     * Resolves a child resource
     *
     * @param path the relative path
     * @return a non-null resource
     */
    Resource resolve(String path);

    /**
     * Returns the URI representing the resource.
     *
     * @return a non-null
     */
    URI toURI();

    /**
     * Creates a copy of the resource, with a different credential.
     *
     * @param credential the credential
     * @return a new instance
     */
    Resource withCredential(Credential credential);

    /**
     * Creates a copy of the resource, with a different name.
     *
     * @param name the name
     * @return a new instance
     */
    Resource withName(String name);

    /**
     * Creates a copy of the resource, with a different description.
     *
     * @param description the description
     * @return a new instance
     */
    Resource withDescription(String description);

    /**
     * Creates a copy of the resource, and adds a new attribute.
     *
     * @param name the name
     * @param name the value
     * @return a new instance
     */
    Resource withAttribute(String name, Object value);

    /**
     * An enum for a resource type
     */
    enum Type {

        /**
         * The resource represents a file
         */
        FILE,

        /**
         * The resource represents a directory
         */
        DIRECTORY
    }
}