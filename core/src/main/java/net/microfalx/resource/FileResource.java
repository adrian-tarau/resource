package net.microfalx.resource;

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

import static net.microfalx.resource.ResourceUtils.*;

/**
 * A resource implementation on top of a {@link File} reference.
 */
public class FileResource extends AbstractResource {

    static final long serialVersionUID = 8384627536253212324L;

    private final File file;

    /**
     * Create a new resource from a file and with a relative path to an arbitrary root.
     *
     * @param uri the URI of the resource
     * @return a non-null instance
     */
    public static Resource create(URI uri) {
        requireNonNull(uri);

        File file = new File(uri.getPath());
        Type type = Type.FILE;
        if (file.isDirectory()) {
            type = Type.DIRECTORY;
        }

        return new FileResource(type, file.getAbsolutePath(), file);
    }

    /**
     * Create a new resource from a file.
     * <p>
     * The type of the resource is based on the file attributes. If the file/directory does not exist, it is presumed a file.
     *
     * @param file the file of the resource
     * @return a non-null instance
     */
    public static Resource create(File file) {
        requireNonNull(file);

        Type type = Type.FILE;
        if (file.exists() && file.isDirectory()) {
            type = Type.DIRECTORY;
        }

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
     * @return the resource mappend to a file
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
        if (parentFile == null) {
            return null;
        }
        return FileResource.create(parentFile);
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
        return getBufferedOutputStream(file);
    }

    @Override
    public final void doCreate() throws IOException {
        if (exists()) return;
        if (getType() == Type.FILE) {
            appendStream(getWriter(), new StringReader(EMPTY_STRING));
        } else {
            if (!file.mkdirs()) {
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
        Path rootPath = file.toPath();
        Stream<Path> walk = Files.walk(rootPath, maxDepth, FileVisitOption.FOLLOW_LINKS);
        AtomicBoolean shouldContinue = new AtomicBoolean(true);
        walk.forEach(path -> {
            boolean same = path.equals(rootPath);
            if (!same && shouldContinue.get()) {
                File child = path.toFile();
                try {
                    shouldContinue.set(visitor.onResource(this, child.isFile() ? FileResource.file(child) : FileResource.directory(child)));
                } catch (IOException e) {
                    ResourceUtils.throwException(e);
                }
            }
        });
        return shouldContinue.get();
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
    public final Resource resolve(String path) {
        requireNonNull(path);
        File child = new File(file, path);
        return FileResource.create(child);
    }

    @Override
    public final Resource resolve(String path, Type type) {
        requireNonNull(path);
        requireNonNull(type);
        File child = new File(file, path);
        return new FileResource(type, hash(child.getAbsolutePath()), child);
    }

    @Override
    public final URI toURI() {
        return file.toURI();
    }

    public static class FileResourceResolver implements ResourceResolver {

        @Override
        public boolean supports(URI uri) {
            String scheme = uri.getScheme();
            return scheme == null || FILE_SCHEME.equalsIgnoreCase(scheme);
        }

        @Override
        public Resource resolve(URI uri) {
            return FileResource.create(uri);
        }
    }
}
