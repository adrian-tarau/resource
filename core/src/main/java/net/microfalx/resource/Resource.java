package net.microfalx.resource;

import java.io.*;
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
     * Returns whether the path represents an absolute path inside the file system supporting this resource.
     *
     * @return {@code true} if absolute, {@code false} otherise
     */
    boolean isAbsolutePath();

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
     * Returns whether the resource can be read.
     *
     * @see #getInputStream()
     * @see #exists()
     */
    default boolean isReadable() {
        return getType() == Type.FILE;
    }

    /**
     * Returns the resource input stream.
     *
     * @return a non-null stream
     * @throws IOException if an I/O error occurs
     */
    InputStream getInputStream() throws IOException;

    /**
     * Returns the resource reader, with a {@link  java.nio.charset.StandardCharsets#UTF_8} encoding
     *
     * @return a non-null instance
     * @throws IOException if an I/O error occurs
     */
    Reader getReader() throws IOException;

    /**
     * Returns whether the resource can be written.
     * <p>
     * Some resources can read-only even if the resources could be changed.
     *
     * @see #getOutputStream()
     * @see #isReadable()
     */
    default boolean isWritable() {
        return getType() == Type.FILE;
    }

    /**
     * Return an {@link OutputStream} for the underlying resource,
     * allowing to (over-)write its content.
     *
     * @throws IOException if the stream could not be opened
     * @see #getInputStream()
     */
    OutputStream getOutputStream() throws IOException;

    /**
     * Returns the resource writer, with a {@link  java.nio.charset.StandardCharsets#UTF_8} encoding
     *
     * @return a non-null instance
     * @throws IOException if an I/O error occurs
     */
    Writer getWriter() throws IOException;

    /**
     * Creates a resource, if missing.
     * <p>
     * If the resource type is a file, an empty file is created, otherwise a directory.
     *
     * @return self
     * @throws IOException if an I/O error occurs
     */
    Resource create() throws IOException;

    /**
     * Deletes a resource, if exists.
     *
     * @return self
     * @throws IOException if an I/O error occurs
     */
    Resource delete() throws IOException;

    /**
     * Deletes the children, if any.
     *
     * @return self
     * @throws IOException if an I/O error occurs
     */
    Resource empty() throws IOException;

    /**
     * Returns whether this resource exists.
     *
     * @return {@code true} if it exists, {@code false} if not.
     */
    boolean exists() throws IOException;

    /**
     * Returns the last modified timestamp for the resource.
     *
     * @return a positive integer, a negative integer if unknown
     */
    long lastModified() throws IOException;

    /**
     * Returns the size of the resource.
     *
     * @return a positive integer, a negative integer if unknown
     */
    long length() throws IOException;

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
    Collection<Resource> list() throws IOException;

    /**
     * Resolves a child resource.
     *
     * @param path the relative path
     * @return a non-null resource
     */
    Resource resolve(String path);

    /**
     * Resolves a child resource.
     *
     * @param path the relative path
     * @param type the type of the child
     * @return a non-null resource
     */
    Resource resolve(String path, Type type);

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
     * Creates a copy of the resource, with a different absolute path.
     *
     * @param absolutePath the name
     * @return a new instance
     */
    Resource withAbsolutePath(boolean absolutePath);

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
     * @param name  the name
     * @param value the value
     * @return a new instance
     */
    Resource withAttribute(String name, Object value);

    /**
     * An enum for a resource type
     */
    enum Type {

        /**
         * The resource represents a file (a leaf)
         */
        FILE,

        /**
         * The resource represents a directory (a container)
         */
        DIRECTORY
    }
}