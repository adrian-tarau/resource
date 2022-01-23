package net.tarau.resource;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static net.tarau.resource.ResourceUtils.*;

/**
 * A resource implementation on top of a {@link File} reference.
 */
public final class FileResource extends AbstractResource {

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
     * Create a new resource from a file and with a relative path to an arbitrary root.
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

    private FileResource(Type type, String id, File file) {
        super(type, id);

        requireNonNull(file);
        this.file = file;
    }

    @Override
    public Resource getParent() {
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
    public File getFile() {
        return file;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return getBufferedInputStream(file);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return getBufferedOutputStream(file);
    }

    @Override
    public void create() throws IOException {
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
    public String getFileName() {
        return file.getName();
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Override
    public long length() {
        return file.length();
    }

    @Override
    public Collection<Resource> list() {
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
    public Resource resolve(String path) {
        requireNonNull(path);
        File child = new File(file, path);
        return FileResource.create(child);
    }

    @Override
    public Resource resolve(String path, Type type) {
        requireNonNull(path);
        requireNonNull(type);
        File child = new File(file, path);
        return new FileResource(type, hash(child.getAbsolutePath()), child);
    }

    @Override
    public URI toURI() {
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
