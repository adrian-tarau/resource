package net.microfalx.resource;

import net.microfalx.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.resource.ResourceUtils.hash;

/**
 * A resource which is resolved as a relative path inside another resource.
 *
 * @see ResourceFactory#getRoot()
 */
public class SharedResource extends AbstractResource {

    private final String path;

    /**
     * Create a new shared resource from a file.
     *
     * @param path the path (relative to the root) of the resource
     * @return a non-null instance
     * @see ResourceFactory#getRoot()
     */
    public static Resource create(String path) {
        requireNonNull(path);
        return path.endsWith("/") ? directory(path) : file(path);
    }

    /**
     * Create a new shared resource from a file.
     *
     * @param path the path (relative to the root) of the resource
     * @return a non-null instance
     * @see ResourceFactory#getRoot()
     */
    public static Resource file(String path) {
        requireNonNull(path);
        return new SharedResource(Type.FILE, hash(path), path);
    }

    /**
     * Create a new shared resource from a directory.
     *
     * @param path the path (relative to the root) of the resource
     * @return a non-null instance
     * @see ResourceFactory#getRoot()
     */
    public static Resource directory(String path) {
        requireNonNull(path);
        return new SharedResource(Type.DIRECTORY, hash(path), path);
    }

    SharedResource(Type type, String id, String path) {
        super(type, id);
        this.path = StringUtils.addStartSlash(path);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getFileName() {
        return getDelegatingResource().getFileName();
    }

    @Override
    public Resource resolve(String path) {
        return getDelegatingResource().resolve(path);
    }

    @Override
    protected InputStream doGetInputStream(boolean raw) throws IOException {
        return getDelegatingResource().getInputStream(raw);
    }

    @Override
    protected OutputStream doGetOutputStream() throws IOException {
        return getDelegatingResource().getOutputStream();
    }

    @Override
    protected void doDelete() throws IOException {
        getDelegatingResource().delete();
    }

    @Override
    protected void doCreate() throws IOException {
        getDelegatingResource().create();
    }

    @Override
    protected boolean doExists() throws IOException {
        return getDelegatingResource().exists();
    }

    @Override
    protected long doLastModified() throws IOException {
        return getDelegatingResource().lastModified();
    }

    @Override
    protected long doLength() throws IOException {
        return getDelegatingResource().length();
    }

    @Override
    protected Collection<Resource> doList() throws IOException {
        return getDelegatingResource().list();
    }

    @Override
    protected boolean doWalk(ResourceVisitor visitor, int maxDepth) throws IOException {
        return getDelegatingResource().walk(visitor, maxDepth);
    }

    @Override
    public URI toURI() {
        return URI.create(ResourceUtils.SHARED + ":" + StringUtils.addStartSlash(path));
    }

    private Resource getDelegatingResource() {
        Resource root = ResourceFactory.getRoot();
        if (root == null) {
            throw new ResourceException("The root of the shared resources is not set");
        }
        return root.resolve(path, getType());
    }
}
