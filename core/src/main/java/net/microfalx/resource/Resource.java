package net.microfalx.resource;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.Collection;

/**
 * An interface for a resource descriptor that abstracts from the actual
 * type of underlying resource, such as a file or class path resource.
 * <p>
 * The object is immutable after creation, but it has methods to create copies and change some attributes.
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
     * Returns whether the resource is of {@link Type#FILE} type.
     *
     * @return <code>true</code> if it is a file, <code>false</code> otherwise
     */
    default boolean isFile() {
        return getType() == Type.FILE;
    }

    /**
     * Returns whether the resource is of {@link Type#FILE} type.
     *
     * @return <code>true</code> if it is a directory, <code>false</code> otherwise
     */
    default boolean isDirectory() {
        return getType() == Type.DIRECTORY;
    }

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
     * @return a non-null string if an extension exists, null otherwise
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
     * <p>
     * The resource can pre-process the stream, like decompress or transform. If raw data is needed, use
     * * {@link #loadAsString(boolean)} and ask for <code>raw</code> bytes.
     *
     * @return The string contents of the resource.
     */
    String loadAsString() throws IOException;

    /**
     * Loads this resource as a string.
     *
     * @param raw <code>true</code> to return the stream as is, <code>false</code> to pre-process the stream (if it applies)
     * @return The string contents of the resource.
     */
    String loadAsString(boolean raw) throws IOException;

    /**
     * Loads this resource as a byte array.
     * <p>
     * The resource can pre-process the stream, like decompress or transform. If raw data is needed, use
     * * {@link #loadAsBytes(boolean)} and ask for <code>raw</code> bytes.
     *
     * @return The contents of the resource.
     */
    byte[] loadAsBytes() throws IOException;

    /**
     * Loads this resource as a byte array.
     *
     * @param raw <code>true</code> to return the stream as is, <code>false</code> to pre-process the stream (if it applies)
     * @return The contents of the resource.
     */
    byte[] loadAsBytes(boolean raw) throws IOException;

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
     * <p>
     * The resource can pre-process the stream, like decompress or transform. If raw data is needed, use
     * {@link #getInputStream(boolean)} and ask for <code>raw</code> bytes.
     *
     * @return a non-null stream
     * @throws IOException if an I/O error occurs
     */
    default InputStream getInputStream() throws IOException {
        return getInputStream(false);
    }

    /**
     * Returns the resource input stream.
     *
     * @param raw <code>true</code> to return the stream as is, <code>false</code> to pre-process the stream (if it applies)
     * @return a non-null stream
     * @throws IOException if an I/O error occurs
     */
    InputStream getInputStream(boolean raw) throws IOException;

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
     * <p>
     * If the resource is a file, it removes the file, otherwise it will recursively remove all
     * children and then remove the directory.
     *
     * @return self
     * @throws IOException if an I/O error occurs
     */
    Resource delete() throws IOException;

    /**
     * Deletes the children, if the resource is a directory (it is ignored for a file).
     * <p>
     * It leaves the directory in place.
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
     * Returns the mime type (content type) based on the file name extension.
     * <p>
     * The mime type can be changed with {@link #withMimeType(String)}.
     *
     * @return the content type
     */
    String getMimeType();

    /**
     * Returns an attribute associated with the resource.
     *
     * @param name the name
     * @return the value, null if it does not exist
     */
    <T> T getAttribute(String name);

    /**
     * Lists child resources.
     * <p>
     * Listing resources might be inefficient for most resources and {@link #walk} is recommended to be used to process
     * all the children (or some of them, up to a given depth).
     *
     * @return a non-null instance
     */
    Collection<Resource> list() throws IOException;

    /**
     * Walks the children of this resource.
     *
     * @param visitor the callback
     * @return <code>true</code> if the tree was walked completely, <code>false</code> if it was aborted
     */
    boolean walk(ResourceVisitor visitor) throws IOException;

    /**
     * Walks the children of this resource.
     *
     * @param visitor  the callback
     * @param maxDepth the maximum depth
     * @return <code>true</code> if the tree was walked completely, <code>false</code> if it was aborted
     */
    boolean walk(ResourceVisitor visitor, int maxDepth) throws IOException;

    /**
     * Resolves a child resource.
     * <p>
     * The type of the child will be directory if the path ends with "/" or a file otherwise.
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
     * Copies the given resource to this resource.
     * <p>
     * If the source is a directory, it copies all sub-resources.
     *
     * @param resource the resource
     * @return self
     * @see #copyFrom(Resource, int)
     */
    Resource copyFrom(Resource resource);

    /**
     * Copies the given resource (and all sub-resources).
     * <p>
     * If the source is a directory, it copies all sub-resources up to a given depth.
     *
     * @param resource the resource
     * @return self
     */
    Resource copyFrom(Resource resource, int depth);

    /**
     * Returns the URI representing the resource.
     *
     * @return a non-null instance
     */
    URI toURI();

    /**
     * Returns the URL (if possible) representing the resource.
     *
     * @return a non-null instance
     * @throws ResourceException if the resource URI's cannot be converted to an URL
     */
    URL toURL();

    /**
     * Returns the resource representing a local file.
     *
     * @return a non-null instance
     */
    Resource toFile();

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
     * Creates a copy of the resource, and changes the mime type.
     *
     * @param mimeType the name
     * @return a new instance
     */
    Resource withMimeType(String mimeType);

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