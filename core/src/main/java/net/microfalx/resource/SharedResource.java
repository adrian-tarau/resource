package net.microfalx.resource;

import net.microfalx.lang.StringUtils;
import net.microfalx.metrics.Metrics;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.StringUtils.EMPTY_STRING;
import static net.microfalx.lang.StringUtils.addStartSlash;
import static net.microfalx.resource.ResourceUtils.SHARED;
import static net.microfalx.resource.ResourceUtils.hash;

/**
 * A resource which is resolved as a relative path inside another resource.
 *
 * @see ResourceFactory#getShared()
 */
public class SharedResource extends AbstractResource {

    private static final Metrics METRICS = ResourceUtils.METRICS.withGroup("Shared");

    private final String path;
    private final String fragment;

    /**
     * Create a new shared resource from a file.
     *
     * @param path the path (relative to the root) of the resource
     * @return a non-null instance
     * @see ResourceFactory#getShared()
     */
    public static Resource create(String path) {
        requireNonNull(path);
        return ResourceUtils.isDirectory(path) ? directory(path) : file(path);
    }

    /**
     * Create a new shared resource from a file.
     *
     * @param path the path (relative to the root) of the resource
     * @return a non-null instance
     * @see ResourceFactory#getShared()
     */
    public static Resource file(String path) {
        return file(path, null);
    }

    /**
     * Create a new shared resource from a file.
     *
     * @param path the path (relative to the root) of the resource
     * @return a non-null instance
     * @see ResourceFactory#getShared()
     */
    public static Resource file(String path, String fragment) {
        requireNonNull(path);
        return new SharedResource(Type.FILE, path, fragment);
    }

    /**
     * Create a new shared resource from a directory.
     *
     * @param path the path (relative to the root) of the resource
     * @return a non-null instance
     * @see ResourceFactory#getShared()
     */
    public static Resource directory(String path) {
        return directory(path, null);
    }

    /**
     * Create a new shared resource from a directory.
     *
     * @param path the path (relative to the root) of the resource
     * @return a non-null instance
     * @see ResourceFactory#getShared()
     */
    public static Resource directory(String path, String fragment) {
        requireNonNull(path);
        return new SharedResource(Type.DIRECTORY, path, fragment);
    }

    SharedResource(Type type, String path, String fragment) {
        super(type, hash(addStartSlash(path)));
        this.path = addStartSlash(path);
        this.fragment = fragment;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getFileName() {
        return getDelegatingResourceWithSymlink().getFileName();
    }

    @Override
    public Resource resolve(String path, Type type) {
        return new SharedResource(type, getSubPath(path), fragment);
    }

    @Override
    public Resource get(String path, Type type) {
        return new SharedResource(type, path, fragment);
    }

    @Override
    protected InputStream doGetInputStream(boolean raw) throws IOException {
        return getDelegatingResourceWithSymlink().getInputStream(raw);
    }

    @Override
    protected OutputStream doGetOutputStream() throws IOException {
        return getDelegatingResourceWithSymlink().getOutputStream();
    }

    @Override
    protected void doDelete() throws IOException {
        getDelegatingResourceWithSymlink().delete();
    }

    @Override
    protected void doCreate() throws IOException {
        getDelegatingResourceWithSymlink().create();
    }

    @Override
    protected boolean doExists() throws IOException {
        return getDelegatingResourceWithSymlink().exists();
    }

    @Override
    protected long doLastModified() throws IOException {
        return getDelegatingResourceWithSymlink().lastModified();
    }

    @Override
    protected long doLength() throws IOException {
        return getDelegatingResourceWithSymlink().length();
    }

    @Override
    protected Collection<Resource> doList() throws IOException {
        return getDelegatingResourceWithSymlink().list();
    }

    @Override
    protected boolean doWalk(ResourceVisitor visitor, int maxDepth) throws IOException {
        return getDelegatingResourceWithSymlink().walk(visitor, maxDepth);
    }

    @Override
    public URI toURI() {
        String fragmentUri = StringUtils.isNotEmpty(fragment) ? "#" + fragment : EMPTY_STRING;
        return URI.create(SHARED + ":" + addStartSlash(path) + fragmentUri);
    }

    @Override
    public Resource toFile() {
        return getDelegatingResourceWithSymlink().toFile();
    }

    @Override
    public boolean isLocal() {
        return getDelegatingResourceWithoutSymlink().isLocal();
    }

    private Resource getDelegatingResourceWithSymlink() {
        return getDelegatingResource(true);
    }

    private Resource getDelegatingResourceWithoutSymlink() {
        return getDelegatingResource(false);
    }

    /**
     * Returns the resource which supports the shared resource.
     *
     * @param resolveSymlinks {@code true} to resolve symlinks, {@code false} otherwise
     * @return a non-null instance
     */
    public Resource getDelegatingResource(boolean resolveSymlinks) {
        Resource root = ResourceFactory.getShared();
        if (root == null) {
            throw new ResourceException("The root of the shared resources is not set");
        }
        if (resolveSymlinks) {
            Resource symlinkedResource = ResourceFactory.resolveSymlink(path, getType());
            if (symlinkedResource != null) return symlinkedResource;
        }
        return root.resolve(path, getType());
    }

    @Override
    protected Metrics getMetrics() {
        return METRICS;
    }

    public static class SharedResourceResolver implements ResourceResolver {

        @Override
        public boolean supports(URI uri) {
            String scheme = uri.getScheme();
            return SHARED.equalsIgnoreCase(scheme);
        }

        @Override
        public Resource resolve(URI uri, Resource.Type type) {
            Resource resource = type != null ? new SharedResource(type, uri.getPath(), uri.getFragment()) : SharedResource.create(uri.getPath());
            return resource.withFragment(uri.getFragment());
        }
    }
}
