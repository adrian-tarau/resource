package net.microfalx.resource;

import net.microfalx.metrics.Metrics;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URI;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static java.nio.file.attribute.FileTime.fromMillis;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.ExceptionUtils.throwException;
import static net.microfalx.lang.IOUtils.*;
import static net.microfalx.lang.StringUtils.EMPTY_STRING;
import static net.microfalx.lang.StringUtils.removeStartSlash;
import static net.microfalx.resource.ResourceUtils.FILE_SCHEME;
import static net.microfalx.resource.ResourceUtils.hash;

/**
 * A resource implementation on top of a {@link File} reference.
 */
public class FileResource extends AbstractResource {

    private static final long serialVersionUID = 8384627536253212324L;

    private static final Metrics METRICS = ResourceUtils.METRICS.withGroup("File");

    private final File file;

    /**
     * Create a new resource from a URI path.
     * <p>
     * The type of the resource is based on the format of the path. If the path is ends with "/", it is a directory,
     * otherwise a file.
     *
     * @param uri the URI of the resource
     * @return a non-null instance
     */
    public static Resource create(URI uri) {
        requireNonNull(uri);
        File file = new File(uri.getPath());
        Type type = Type.FILE;
        if (ResourceUtils.isDirectory(file, true)) type = Type.DIRECTORY;
        return new FileResource(type, file.getAbsolutePath(), file);
    }

    /**
     * Create a new resource from a URI path.
     * <p>
     * The type of the resource is based on the format of the path. If the path is ends with "/", it is a directory,
     * otherwise a file.
     *
     * @param uri  the URI of the resource
     * @param type the resource type
     * @return a non-null instance
     */
    public static Resource create(URI uri, Resource.Type type) {
        requireNonNull(uri);
        requireNonNull(type);
        File file = new File(uri.getPath());
        return new FileResource(type, file.getAbsolutePath(), file);
    }

    /**
     * Create a new resource from a file.
     * <p>
     * The type of the resource is based on the file attributes/path. If the file/directory does not exist, the path is
     * validated if it ends with "/".
     *
     * @param file the file of the resource
     * @return a non-null instance
     * @see ResourceUtils#isDirectory(File, boolean)
     */
    public static Resource create(File file) {
        requireNonNull(file);
        Type type = Type.FILE;
        if (ResourceUtils.isDirectory(file, true)) type = Type.DIRECTORY;
        return new FileResource(type, hash(file.getAbsolutePath()), file);
    }

    /**
     * Create a new resource from a file.
     *
     * @param file the file of the resource
     * @return a non-null instance
     */
    public static Resource file(File file) {
        requireNonNull(file);
        return new FileResource(Type.FILE, hash(file.getAbsolutePath()), file);
    }

    /**
     * Create a new resource from a directory.
     *
     * @param file the file of the resource
     * @return a non-null instance
     */
    public static Resource directory(File file) {
        requireNonNull(file);
        return new FileResource(Type.DIRECTORY, hash(file.getAbsolutePath()), file);
    }

    /**
     * Creates a file resource out of another resource.
     *
     * @param resource the original resource.
     * @return the resource mapped to a file
     * @throws IOException if an I/O error occurs
     */
    public static Resource create(Resource resource) throws IOException {
        requireNonNull(resource);
        if (resource instanceof FileResource) {
            return resource;
        } else {
            File tmpFile = File.createTempFile("file_resource_", "_" + resource.getFileName());
            appendStream(getBufferedOutputStream(tmpFile), resource.getInputStream());
            return create(tmpFile);
        }
    }

    protected FileResource(Type type, String id, File file) {
        super(type, id);
        requireNonNull(file);
        this.file = file;
    }

    @Override
    public final Resource getParent() {
        File parentFile = file.getParentFile();
        return parentFile == null ? null : FileResource.directory(parentFile);
    }

    /**
     * Retrieves the location of this resource on disk (absolute path).
     *
     * @return The location of this resource on disk.
     */
    public final File getFile() {
        return file;
    }

    @Override
    public final InputStream doGetInputStream(boolean raw) throws IOException {
        return getBufferedInputStream(file);
    }

    @Override
    public final OutputStream doGetOutputStream() throws IOException {
        createParent();
        return getBufferedOutputStream(file);
    }

    @Override
    public final void doCreate() throws IOException {
        if (exists()) return;
        if (getType() == Type.FILE) {
            createParent();
            appendStream(getWriter(), new StringReader(EMPTY_STRING));
        } else {
            if (!ResourceUtils.retryWithStatus(this, resource -> file.mkdirs())) {
                throw new IOException("Directory '" + file + "' cannot be created");
            }
        }
    }

    @Override
    protected final void doDelete() throws IOException {
        if (file.exists()) {
            Files.delete(file.toPath());
        }
    }

    @Override
    protected boolean doWalk(ResourceVisitor visitor, int maxDepth) throws IOException {
        if (!file.exists()) return true;
        Path rootPath = file.toPath();
        AtomicBoolean shouldContinue = new AtomicBoolean(true);
        try (Stream<Path> walk = Files.walk(rootPath, maxDepth, FileVisitOption.FOLLOW_LINKS)) {
            walk.forEach(path -> {
                boolean same = path.equals(rootPath);
                if (!same && shouldContinue.get()) {
                    File child = path.toFile();
                    try {
                        shouldContinue.set(visitor.onResource(this, child.isFile() ? FileResource.file(child) : FileResource.directory(child)));
                    } catch (IOException e) {
                        throwException(e);
                    }
                }
            });
            return shouldContinue.get();
        }
    }

    @Override
    public final String getFileName() {
        return file.getName();
    }

    @Override
    public final boolean doExists() {
        return file.exists();
    }

    @Override
    protected final long doLastModified() {
        return file.lastModified();
    }

    @Override
    protected final long doLength() {
        return file.length();
    }

    @Override
    protected final Collection<Resource> doList() {
        String[] fileNames = file.list();
        if (fileNames == null) {
            return Collections.emptyList();
        }
        Collection<Resource> resources = new ArrayList<>();
        for (String fileName : fileNames) {
            File child = new File(file, fileName);
            resources.add(FileResource.create(child));
        }
        return resources;
    }

    @Override
    protected void doCopyPropertiesFrom(Resource resource) throws IOException {
        Files.setLastModifiedTime(file.toPath(), fromMillis(resource.lastModified()));
    }

    @Override
    public final Resource resolve(String path) {
        requireNonNull(path);
        File child = new File(file, removeStartSlash(path));
        return FileResource.create(child);
    }

    @Override
    public final Resource resolve(String path, Type type) {
        requireNonNull(path);
        requireNonNull(type);
        File child = new File(file, removeStartSlash(path));
        return new FileResource(type, hash(child.getAbsolutePath()), child);
    }

    @Override
    public Resource get(String path) {
        requireNonNull(path);
        File child = new File(path);
        return FileResource.create(child);
    }

    @Override
    public Resource get(String path, Type type) {
        requireNonNull(path);
        requireNonNull(type);
        File child = new File(path);
        return new FileResource(type, hash(child.getAbsolutePath()), child);
    }

    @Override
    public final URI toURI() {
        return file.toURI();
    }

    @Override
    public final Resource toFile() {
        return this;
    }

    @Override
    protected Metrics getMetrics() {
        return METRICS;
    }

    @Override
    protected void afterEmpty() throws IOException {
        super.afterEmpty();
        if (file.isDirectory()) FileUtils.cleanDirectory(file);
    }

    private void createParent() throws IOException {
        if (!ResourceUtils.retryWithStatus(this, resource -> file.getParentFile().exists() || file.getParentFile().mkdirs())) {
            throw new IOException("Parent directory ('" + file.getParentFile() + "') does not exist and could not be created");
        }

    }

    public static class FileResourceResolver implements ResourceResolver {

        @Override
        public boolean supports(URI uri) {
            String scheme = uri.getScheme();
            return scheme == null || FILE_SCHEME.equalsIgnoreCase(scheme);
        }

        @Override
        public Resource resolve(URI uri, Resource.Type type) {
            Resource resource = type != null ? FileResource.create(uri, type) : FileResource.create(uri);
            return resource.withFragment(uri.getFragment());
        }
    }
}
