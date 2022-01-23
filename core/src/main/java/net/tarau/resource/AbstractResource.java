package net.tarau.resource;

import java.io.*;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static net.tarau.resource.ResourceUtils.*;

/**
 * A skeleton implementation for a resource.
 */
public abstract class AbstractResource implements Resource, Cloneable {

    private final Type type;
    private final String id;

    private String name;
    private String description;
    private boolean absolutePath = true;

    private Credential credential = new NullCredential();

    private Map<String, Object> attributes;

    protected AbstractResource(Type type, String id) {
        requireNonNull(type);
        requireNonNull(id);

        this.type = type;
        this.id = id;
    }

    @Override
    public final Type getType() {
        return type;
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public final String getName() {
        if (isNotEmpty(name)) {
            return name;
        }
        return getFileName();
    }

    @Override
    public final Credential getCredential() {
        return credential;
    }

    protected final void setCredential(Credential credential) {
        requireNonNull(credential);
        this.credential = credential;
    }

    @Override
    public Resource getParent() {
        return null;
    }

    @Override
    public <T> T getAttribute(String name) {
        requireNonNull(name);

        if (attributes == null) {
            return null;
        }
        return (T) attributes.get(name);
    }

    @Override
    public Reader getReader() throws IOException {
        return new InputStreamReader(getInputStream(), StandardCharsets.UTF_8);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Not supported");
    }

    @Override
    public Writer getWriter() throws IOException {
        return new OutputStreamWriter(getOutputStream(), StandardCharsets.UTF_8);
    }

    @Override
    public void create() throws IOException {
        throw new IOException("Not supported");
    }

    @Override
    public Resource resolve(String path, Type type) {
        return resolve(path);
    }

    @Override
    public Resource withCredential(Credential credential) {
        requireNonNull(credential);
        AbstractResource copy = copy();
        copy.credential = credential;
        return copy;
    }

    @Override
    public Resource withAbsolutePath(boolean absolutePath) {
        AbstractResource copy = copy();
        copy.absolutePath = absolutePath;
        return copy;
    }

    public final Resource withName(String name) {
        requireNonNull(name);
        AbstractResource copy = copy();
        copy.name = name;
        return copy;
    }

    @Override
    public String getPath(Resource resource) {
        requireNonNull(resource);

        String resourcePath = removeEndSlash(resource.toURI().toASCIIString());
        String path = removeEndSlash(this.toURI().toASCIIString());
        if (path.length() > resourcePath.length()) {
            return path.substring(resourcePath.length() + 1);
        }

        return EMPTY_STRING;
    }

    @Override
    public String getPath() {
        URI uri = toURI();
        if ("jar".equalsIgnoreCase(uri.getScheme())) {
            String _uri = uri.toASCIIString();
            int lastIndex = _uri.lastIndexOf("!/");
            if (lastIndex != -1) {
                return _uri.substring(lastIndex + 1);
            } else {
                return "/";
            }
        }
        String path = uri.getPath();
        if (isEmpty(path)) {
            path = "/";
        }
        return path;
    }

    @Override
    public boolean isAbsolutePath() {
        return absolutePath;
    }

    protected final void setAbsolutePath(boolean absolutePath) {
        this.absolutePath = absolutePath;
    }

    @Override
    public String getContentType() {
        String contentType = null;
        if (isNotEmpty(getFileExtension())) {
            contentType = URLConnection.guessContentTypeFromName(getFileName());
        }
        return defaultIfEmpty(contentType, "application/octet-stream");
    }

    @Override
    public String getDescription() {
        if (isNotEmpty(description)) {
            return description;
        }
        return getName();
    }

    public final Resource withDescription(String description) {
        AbstractResource copy = copy();
        copy.description = description;
        return copy;
    }

    @Override
    public Resource withAttribute(String name, Object value) {
        AbstractResource copy = copy();
        if (copy.attributes == null) copy.attributes = new HashMap<>();
        copy.attributes.put(name, value);
        return copy;
    }

    @Override
    public final String loadAsString() throws IOException {
        return getInputStreamAsString(getInputStream());
    }

    @Override
    public final byte[] loadAsBytes() throws IOException {
        return getInputStreamAsBytes(getInputStream());
    }

    @Override
    public final String getFileExtension() {
        return ResourceUtils.getFileExtension(getFileName());
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractResource that = (AbstractResource) o;

        if (type != that.type) return false;
        return id.equals(that.id);
    }

    @Override
    public final int hashCode() {
        int result = type.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }

    /**
     * Called when a child is created to update some shared attributes between parent (this instnace) and a child.
     *
     * @param child the child to be updated
     */
    private final void updateChild(Resource child) {
        // empty
    }

    /**
     * Creates a copy of the object
     *
     * @param <T> the type
     * @return a new instance
     */
    protected final <T extends AbstractResource> T copy() {
        try {
            return (T) clone();
        } catch (CloneNotSupportedException e) {
            return throwException(e);
        }
    }

    /**
     * Calculates the type of the resource based on the path.
     *
     * @param path         the path, can be NULL
     * @param currentValue the current value, can be NULL
     * @return the resource type
     */
    protected static Type typeFromPath(String path, Type currentValue) {
        if (isEmpty(path)) return Type.DIRECTORY;
        if (currentValue == null) {
            currentValue = Type.FILE;
            if (path.endsWith("/")) {
                currentValue = Type.DIRECTORY;
            }
        }
        return currentValue;
    }
}
