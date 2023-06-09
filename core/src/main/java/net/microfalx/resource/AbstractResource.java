package net.microfalx.resource;

import net.microfalx.lang.FileUtils;
import net.microfalx.metrics.Metrics;

import java.io.*;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.ExceptionUtils.throwException;
import static net.microfalx.lang.IOUtils.*;
import static net.microfalx.lang.StringUtils.*;
import static net.microfalx.resource.ResourceUtils.METRICS;

/**
 * A skeleton implementation for a resource.
 */
public abstract class AbstractResource implements Resource, Cloneable {

    protected static final int BUFFER_SIZE = 128 * 1024;
    protected static final int MAX_DEPTH = 128;

    private Type type;
    private boolean typeRecalculated;
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
        if (!typeRecalculated) {
            try {
                type = calculateType(type);
            } catch (IOException e) {
                return throwException(e);
            }
            typeRecalculated = true;
        }
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
    public final <T> T getAttribute(String name) {
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
    public final InputStream getInputStream() throws IOException {
        return getInputStream(false);
    }

    @Override
    public InputStream getInputStream(boolean raw) throws IOException {
        return time("get_input", () -> getBufferedInputStream(process(doGetInputStream(raw), raw)));
    }

    @Override
    public final OutputStream getOutputStream() throws IOException {
        return time("get_output", () -> getBufferedOutputStream(doGetOutputStream()));
    }

    /**
     * Subclasses will provide an input stream for this resource.
     *
     * @param raw <code>true</code> to return the stream as is, <code>false</code> to pre-process the stream (if it applies)
     * @return a non-null instance
     * @throws IOException if I/O error occurs
     */
    protected InputStream doGetInputStream(boolean raw) throws IOException {
        throw new IOException("Not supported");
    }

    /**
     * Subclasses will provide an output stream for this resource.
     *
     * @return a non-null instance
     * @throws IOException if I/O error occurs
     */
    protected OutputStream doGetOutputStream() throws IOException {
        throw new IOException("Not supported");
    }

    /**
     * Returns a real type for a resource.
     *
     * @return the new type
     * @throws IOException if I/O error occurs
     */
    protected Type calculateType(Type type) throws IOException {
        return type;
    }

    @Override
    public final Writer getWriter() throws IOException {
        return new OutputStreamWriter(getOutputStream(), StandardCharsets.UTF_8);
    }

    @Override
    public final Resource create() throws IOException {
        return time("create", () -> {
            doCreate();
            return this;
        });
    }

    @Override
    public final Resource delete() throws IOException {
        return time("delete", () -> {
            if (isDirectory()) empty();
            doDelete();
            return this;
        });
    }

    @Override
    public final Collection<Resource> list() throws IOException {
        return time("list", this::doList);
    }

    @Override
    public final boolean walk(ResourceVisitor visitor) throws IOException {
        return walk(visitor, MAX_DEPTH);
    }

    @Override
    public final boolean walk(ResourceVisitor visitor, int maxDepth) throws IOException {
        if (isFile()) return true;
        return time("walk", () -> doWalk(visitor, maxDepth));
    }

    @Override
    public final Resource copyFrom(Resource resource) {
        return copyFrom(resource, MAX_DEPTH);
    }

    @Override
    public final Resource copyFrom(Resource resource, int depth) {
        return time("copy", () -> doCopyFrom(resource, depth));
    }

    @Override
    public final Resource empty() throws IOException {
        if (getType() != Type.DIRECTORY) return this;
        return time("empty", () -> {
            walk((parent, child) -> {
                child.delete();
                return true;
            });
            return this;
        });
    }

    @Override
    public final long lastModified() throws IOException {
        return time("last_modified", this::doLastModified);
    }

    @Override
    public final long length() throws IOException {
        return time("length", this::doLength);
    }

    @Override
    public final boolean exists() throws IOException {
        return time("exists", this::doExists);
    }

    protected void doDelete() throws IOException {
        throw new IOException("Not supported");
    }

    protected void doCreate() throws IOException {
        throw new IOException("Not supported");
    }

    protected boolean doExists() throws IOException {
        throw new IOException("Not supported");
    }

    protected long doLastModified() throws IOException {
        throw new IOException("Not supported");
    }

    protected long doLength() throws IOException {
        throw new IOException("Not supported");
    }

    protected Collection<Resource> doList() throws IOException {
        Collection<Resource> children = new ArrayList<>();
        walk((parent, child) -> children.add(child), 1);
        return Collections.unmodifiableCollection(children);
    }

    protected void doCopyFrom() throws IOException {
        throw new IOException("Not supported");
    }

    protected boolean doWalk(ResourceVisitor visitor, int maxDepth) throws IOException {
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
    public String loadAsString(boolean raw) throws IOException {
        return getInputStreamAsString(getInputStream(raw));
    }

    @Override
    public final byte[] loadAsBytes() throws IOException {
        return getInputStreamAsBytes(getInputStream());
    }

    @Override
    public byte[] loadAsBytes(boolean raw) throws IOException {
        return getInputStreamAsBytes(getInputStream());
    }

    @Override
    public final String getFileExtension() {
        return FileUtils.getFileExtension(getFileName());
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

    @Override
    public Resource toFile() {
        throw new ResourceException("Not Supported");
    }

    /**
     * Called when a child is created to update some shared attributes between parent (this instance) and a child.
     *
     * @param child the child to be updated
     */
    private final void updateChild(Resource child) {
        // empty
    }

    /**
     * Returns the metrics to be used with this instance.
     *
     * @return a non-null instance
     */
    protected Metrics getMetrics() {
        return METRICS;
    }

    /**
     * Actual implementation of copy.
     *
     * @param resource the source resource
     * @param depth    the current depth
     * @return self
     */
    protected Resource doCopyFrom(Resource resource, int depth) {
        return this;
    }

    /**
     * Times a resource operation.
     *
     * @param name     the name
     * @param callable the callable
     * @param <T>      the return type
     * @return the return value
     */
    protected final <T> T time(String name, Callable<T> callable) {
        Metrics metrics = getMetrics();
        return metrics.time(name, callable);
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

    /**
     * Invokes the processors if the content is not raw.
     *
     * @param inputStream the input stream
     * @param raw         {@code true} to ask for raw value, {@code false} otherwise
     * @return an processed stream or the original
     */
    private InputStream process(InputStream inputStream, boolean raw) {
        if (raw) return inputStream;
        return ResourceFactory.process(this, inputStream);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "type=" + getType() +
                ", URI='" + toURI() + '\'' +
                ", name='" + getName() + '\'' +
                ", credential=" + getCredential() +
                '}';
    }

    static {
        ResourceFactory.initialize();
    }
}
