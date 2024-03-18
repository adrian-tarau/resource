package net.microfalx.resource;

import net.microfalx.lang.*;
import net.microfalx.metrics.Metrics;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;

import static java.net.URLConnection.guessContentTypeFromName;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.ExceptionUtils.throwException;
import static net.microfalx.lang.IOUtils.*;
import static net.microfalx.lang.StringUtils.*;
import static net.microfalx.resource.MimeType.APPLICATION_OCTET_STREAM;
import static net.microfalx.resource.ResourceUtils.METRICS;
import static net.microfalx.resource.ResourceUtils.SLASH;

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
    private String mimeType;
    private boolean absolutePath = true;
    private String fragment;

    private Credential credential = Credential.NA;

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
    public Resource getRoot() {
        return get(SLASH, Type.DIRECTORY);
    }

    @Override
    public Resource getParent() {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T> T getAttribute(String name) {
        requireNonNull(name);
        return attributes == null ? null : (T) attributes.get(name);
    }

    @Override
    public final Reader getReader() throws IOException {
        return new InputStreamReader(getInputStream(), StandardCharsets.UTF_8);
    }

    @Override
    public final InputStream getInputStream() throws IOException {
        return getInputStream(false);
    }

    @Override
    public final InputStream getInputStream(boolean raw) throws IOException {
        return time("Get Input", () -> getBufferedInputStream(process(getBufferedInputStream(doGetInputStream(raw)), raw)));
    }

    @Override
    public final OutputStream getOutputStream() throws IOException {
        return time("Get Output", () -> getBufferedOutputStream(doGetOutputStream()));
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
        return time("Create", () -> {
            doCreate();
            return this;
        });
    }

    @Override
    public final Resource createParents() throws IOException {
        return time("Create Parents", () -> {
            doCreateParents();
            return this;
        });
    }

    @Override
    public final Resource delete() throws IOException {
        return time("Delete", () -> {
            if (isDirectory()) empty();
            doDelete();
            return this;
        });
    }

    @Override
    public final Collection<Resource> list() throws IOException {
        return time("List", this::doList);
    }

    @Override
    public final boolean walk(ResourceVisitor visitor) throws IOException {
        return walk(visitor, MAX_DEPTH);
    }

    @Override
    public final boolean walk(ResourceVisitor visitor, int maxDepth) throws IOException {
        if (isFile()) return true;
        return time("Walk", () -> doWalk(visitor, maxDepth));
    }

    @Override
    public final Resource copyFrom(Resource resource) {
        return copyFrom(resource, MAX_DEPTH);
    }

    @Override
    public final Resource copyFrom(Resource resource, int depth) {
        requireNonNull(resource);
        return time("Copy", () -> {
            Resource self = doCopyFrom(resource, depth);
            copyPropertiesFrom(resource);
            return self;
        });
    }

    @Override
    public Resource copyPropertiesFrom(Resource resource) {
        requireNonNull(resource);
        return time("Copy Properties", () -> {
            this.mimeType = resource.getMimeType();
            if (resource instanceof AbstractResource) {
                AbstractResource otherResource = (AbstractResource) resource;
                this.name = otherResource.name;
                this.description = otherResource.description;
                if (otherResource.attributes != null) {
                    if (this.attributes == null) this.attributes = new HashMap<>();
                    otherResource.attributes.putAll(this.attributes);
                }
            }
            return this;
        });
    }

    @Override
    public final Resource empty() throws IOException {
        if (getType() != Type.DIRECTORY) return this;
        return time("Empty", () -> {
            walk((parent, child) -> {
                child.delete();
                return true;
            });
            return this;
        });
    }

    @Override
    public final long lastModified() throws IOException {
        return time("Last Modified", this::doLastModified);
    }

    @Override
    public final long length() throws IOException {
        return time("Length", this::doLength);
    }

    @Override
    public final boolean exists() throws IOException {
        return time("Exists", this::doExists);
    }

    @Override
    public final URL toURL() {
        URI uri = toURI();
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new ResourceException("Failed to convert URI '" + uri + "' to an URL", e);
        }
    }

    protected void doDelete() throws IOException {
        throw new IOException("Not supported");
    }

    protected void doCreate() throws IOException {
        throw new IOException("Not supported");
    }

    protected void doCreateParents() throws IOException {
        Resource parent = getType() == Type.FILE ? getParent() : this;
        if (parent != null) {
            try {
                parent.create();
            } catch (Exception e) {
                parent.createParents();
                parent.create();
            }
        }
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

    protected boolean doWalk(ResourceVisitor visitor, int maxDepth) throws IOException {
        return doWalk(this, this, visitor, 1, maxDepth);
    }

    @Override
    public Resource resolve(String path) {
        Type type = ResourceUtils.isDirectory(path) ? Type.DIRECTORY : Type.FILE;
        return resolve(path, type);
    }

    @Override
    public Resource resolve(String path, Type type) {
        return get(getSubPath(path), type);
    }

    @Override
    public Resource get(String path) {
        Type type = ResourceUtils.isDirectory(path) ? Type.DIRECTORY : Type.FILE;
        return get(path, type);
    }

    @Override
    public final Resource withCredential(Credential credential) {
        requireNonNull(credential);
        AbstractResource copy = copy();
        copy.credential = credential;
        return copy;
    }

    @Override
    public final Resource withAbsolutePath(boolean absolutePath) {
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
    public final String getPath(Resource resource) {
        requireNonNull(resource);
        String resourcePath = removeEndSlash(UriUtils.removeFragment(resource.toURI()).toASCIIString());
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
            String uriAsString = uri.toASCIIString();
            int lastIndex = uriAsString.lastIndexOf("!/");
            if (lastIndex != -1) {
                return uriAsString.substring(lastIndex + 1);
            } else {
                return "/";
            }
        }
        String path = uri.getPath();
        if (isEmpty(path)) path = ResourceUtils.SLASH;
        return path;
    }

    @Override
    public final String getFragment() {
        return fragment;
    }

    /**
     * Changes the fragment associated with the resource
     *
     * @param fragment the fragment
     */
    protected final void setFragment(String fragment) {
        this.fragment = fragment;
    }

    @Override
    public final boolean isAbsolutePath() {
        return absolutePath;
    }

    protected final void setAbsolutePath(boolean absolutePath) {
        this.absolutePath = absolutePath;
    }

    @Override
    public final String getMimeType() {
        if (mimeType == null) mimeType = detectMimeType();
        return mimeType;
    }

    @Override
    public String detectMimeType() {
        String mimeType = null;
        InputStream inputStream = null;
        try {
            inputStream = doGetInputStream(false);
            mimeType = ResourceFactory.detect(inputStream, getFileName());
        } catch (IOException e) {
            // if it cannot resolve the stream, try to file name
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        if (mimeType == null && isNotEmpty(getFileExtension())) return guessContentTypeFromName(getFileName());
        if (StringUtils.isEmpty(mimeType)) mimeType = APPLICATION_OCTET_STREAM.getValue();
        return mimeType;
    }

    @Override
    public final String getDescription() {
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
    public final Resource withAttribute(String name, Object value) {
        AbstractResource copy = copy();
        if (copy.attributes == null) copy.attributes = new HashMap<>();
        copy.attributes.put(name, value);
        return copy;
    }

    @Override
    public final Resource withMimeType(String mimeType) {
        requireNonNull(mimeType);
        AbstractResource copy = copy();
        copy.mimeType = mimeType;
        return copy;
    }

    @Override
    public final Resource withMimeType(MimeType mimeType) {
        AbstractResource copy = copy();
        copy.mimeType = mimeType != null ? mimeType.toString() : null;
        return copy;
    }

    @Override
    public final Resource withFragment(String fragment) {
        if (StringUtils.isEmpty(fragment)) {
            return this;
        } else {
            AbstractResource copy = copy();
            copy.fragment = fragment;
            return copy;
        }
    }

    @Override
    public final String loadAsString() throws IOException {
        return getInputStreamAsString(getInputStream());
    }

    @Override
    public final String loadAsString(boolean raw) throws IOException {
        return getInputStreamAsString(getInputStream(raw));
    }

    @Override
    public final byte[] loadAsBytes() throws IOException {
        return getInputStreamAsBytes(getInputStream());
    }

    @Override
    public final byte[] loadAsBytes(boolean raw) throws IOException {
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
        URI uri = toURI();
        String scheme = uri.getScheme();
        if (scheme == null || "file".equalsIgnoreCase(scheme)) {
            return FileResource.create(uri);
        } else {
            throw new ResourceException(ClassUtils.getName(this) + " cannot be converted to a file resource");
        }
    }

    @Override
    public boolean isLocal() {
        URI uri = toURI();
        String scheme = uri.getScheme();
        return scheme == null || "file".equalsIgnoreCase(scheme);
    }

    /**
     * Called when a child is created to update some shared attributes between parent (this instance) and a child.
     *
     * @param child the child to be updated
     */
    private void updateChild(Resource child) {
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
    protected Resource doCopyFrom(Resource resource, int depth) throws IOException {
        createParents();
        if (resource.isFile()) {
            IOUtils.appendStream(getOutputStream(), resource.getInputStream(true));
        } else {
            throw new ResourceException("Directory copy not supported");
        }
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
        return metrics.timeCallable(name, callable);
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
     * Returns whether the resource has a fragment attached to it.
     *
     * @return {@code true} if a fragment is present, {@code false} otherwise
     */
    protected final boolean hasFragment() {
        return StringUtils.isNotEmpty(getFragment());
    }

    /**
     * Create an URI for this resource without the fragment.
     *
     * @return a non-null instance
     */
    protected final URI toURINoFragment() {
        return UriUtils.removeFragment(toURI());
    }

    /**
     * Returns a sub-path relative to the resource path.
     *
     * @param path the sub path
     * @return a non-null instance
     */
    protected String getSubPath(String path) {
        return removeEndSlash(this.getPath()) + ResourceUtils.SLASH + removeStartSlash(path);
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

    /**
     * Default implementation of the walk, using {@link #list()}
     *
     * @param resource     the current resource
     * @param visitor      the visitor
     * @param currentDepth the current depth
     * @param maxDepth     the maximum depth
     * @return <code>true</code> if the tree was walked completely, <code>false</code> if it was aborted
     * @throws IOException if an I/O error occurs
     */
    private boolean doWalk(Resource rootResource, Resource resource, ResourceVisitor visitor, int currentDepth, int maxDepth) throws IOException {
        if (currentDepth > maxDepth) return false;
        Collection<Resource> childResources = resource.list();
        for (Resource childResource : childResources) {
            visitor.onResource(rootResource, childResource);
            if (childResource.isDirectory()) {
                if (doWalk(rootResource, childResource, visitor, currentDepth + 1, maxDepth)) return true;
            }
        }
        return false;
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
