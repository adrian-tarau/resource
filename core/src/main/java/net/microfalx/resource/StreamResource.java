package net.microfalx.resource;


import net.microfalx.lang.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Callable;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.IOUtils.getBufferedInputStream;
import static net.microfalx.lang.StringUtils.isEmpty;

/**
 * A resource based on an {@link InputStream}.
 * <p>
 * The stream can be consumed once.
 */
public final class StreamResource extends AbstractResource {

    static final long serialVersionUID = 4387627536253212324L;

    private final InputStream inputStream;
    private final String fileName;
    private Callable<InputStream> supplier;
    private final long lastModified = System.currentTimeMillis();
    private volatile boolean streamConsumed;

    /**
     * Creates a new resource.
     *
     * @param inputStream the input stream used as content for resource
     * @return a non-null instance
     */
    public static Resource create(InputStream inputStream) {
        requireNonNull(inputStream);
        String id = UUID.randomUUID().toString();
        return create(inputStream, id, null);
    }

    /**
     * Creates a new resource.
     *
     * @param inputStream the input stream used as content for resource
     * @return a non-null instance
     */
    public static Resource create(InputStream inputStream, String fileName) {
        requireNonNull(inputStream);
        return create(inputStream, fileName, null);
    }

    /**
     * Creates a new resource.
     *
     * @param callable the supplier of the input stream
     * @return a non-null instance
     */
    public static Resource create(Callable<InputStream> callable) {
        requireNonNull(callable);
        String id = UUID.randomUUID().toString();
        return create(null, id, null);
    }

    /**
     * Creates a new resource.
     *
     * @param callable the supplier of the input stream
     * @return a non-null instance
     */
    public static Resource create(Callable<InputStream> callable, String fileName) {
        requireNonNull(callable);
        return create(null, fileName, callable);
    }

    /**
     * Creates a new resource from a byte array.
     *
     * @param inputStream the input stream used as content for resource
     * @param fileName    the file name associated with the memory stream
     * @param supplier    the supplier for the input streams
     * @return a non-null instance
     */
    private static Resource create(InputStream inputStream, String fileName, Callable<InputStream> supplier) {
        String id = UUID.randomUUID().toString();
        if (isEmpty(fileName)) fileName = id;
        return new StreamResource(inputStream, id, fileName, supplier);
    }

    private StreamResource(InputStream inputStream, String id, String fileName, Callable<InputStream> supplier) {
        super(Type.FILE, id);
        this.inputStream = inputStream != null ? getBufferedInputStream(inputStream) : null;
        this.supplier = supplier;
        this.fileName = fileName;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public InputStream doGetInputStream(boolean raw) throws IOException {
        if (inputStream != null) {
            if (streamConsumed) throw new IOException("Stream '" + getFileName() + "' already consumed");
            try {
                return inputStream;
            } finally {
                streamConsumed = true;
            }
        } else {
            try {
                return supplier.call();
            } catch (Exception e) {
                throw new IOException("Failed to retrieve a stream from supplier '" + ClassUtils.getName(supplier) + "'", e);
            }
        }
    }

    @Override
    public boolean doExists() {
        return true;
    }

    @Override
    protected long doLastModified() {
        return lastModified;
    }

    @Override
    protected long doLength() {
        return -1;
    }

    @Override
    protected Collection<Resource> doList() {
        return Collections.emptyList();
    }

    @Override
    public Resource resolve(String path) {
        return NullResource.createNull();
    }

    @Override
    public URI toURI() {
        try {
            return new URI("memory://" + getId() + "/" + getFileName());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
