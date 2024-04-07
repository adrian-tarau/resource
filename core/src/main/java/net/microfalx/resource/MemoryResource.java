package net.microfalx.resource;

import net.microfalx.lang.Hashing;
import net.microfalx.metrics.Metrics;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.IOUtils.getInputStreamAsBytes;

/**
 * A resource which is stored in memory.
 */
public final class MemoryResource extends AbstractResource {

    static final long serialVersionUID = -2384762736253212324L;

    private static final Metrics METRICS = ResourceUtils.METRICS.withGroup("Memory");

    private byte[] data;
    private final String fileName;
    private final long lastModified;

    private boolean writable = true;

    /**
     * Creates a resource from another resource.
     *
     * @param resource the resource
     * @return a non-null instance
     * @throws IOException if an I/O occurs
     */
    public static Resource create(Resource resource) throws IOException {
        byte[] data = getInputStreamAsBytes(resource.getInputStream());
        return create(data, resource.getName());
    }

    /**
     * Creates a new resource from a text.
     *
     * @param text the text
     * @return a non-null instance
     */
    public static Resource create(String text) {
        requireNonNull(text);
        MemoryResource resource = (MemoryResource) create(text.getBytes()).withMimeType(MimeType.TEXT_PLAIN);
        resource.setName(ResourceUtils.createName(text));
        return resource;
    }

    /**
     * Creates a new resource from a text.
     *
     * @param text     the text
     * @param fileName the file name associated with the memory stream
     * @return a non-null instance
     */
    public static Resource create(String text, String fileName) {
        requireNonNull(text);
        return create(text.getBytes(), fileName).withMimeType(MimeType.TEXT_PLAIN);
    }

    /**
     * Creates a new resource from a text.
     *
     * @param text         the text
     * @param fileName     the file name associated with the memory stream
     * @param lastModified the timestamp when the resource was changed
     * @return a non-null instance
     */
    public static Resource create(String text, String fileName, long lastModified) {
        requireNonNull(text);
        return create(text.getBytes(), fileName, lastModified).withMimeType(MimeType.TEXT_PLAIN);
    }

    /**
     * Creates a new resource from a byte array.
     *
     * @param data the array used as content for resource
     * @return a non-null instance
     */
    public static Resource create(byte[] data) {
        String id = UUID.randomUUID().toString();
        return create(data, id);
    }

    /**
     * Creates a new resource from a byte array.
     *
     * @param data     the array used as content for resource
     * @param fileName the file name associated with the memory stream
     * @return a non-null instance
     */
    public static Resource create(byte[] data, String fileName) {
        String id = UUID.randomUUID().toString();
        return new MemoryResource(data, id, fileName, System.currentTimeMillis());
    }

    /**
     * Creates a new resource from a byte array.
     *
     * @param data         the array used as content for resource
     * @param fileName     the file name associated with the memory stream
     * @param lastModified the timestamp when the resource was changed
     * @return a non-null instance
     */
    public static Resource create(byte[] data, String fileName, long lastModified) {
        String id = UUID.randomUUID().toString();
        return new MemoryResource(data, id, fileName, lastModified);
    }

    private MemoryResource(byte[] data, String id, String fileName, long lastModified) {
        super(Type.FILE, id);
        requireNonNull(data);
        this.fileName = fileName;
        this.data = Arrays.copyOf(data, data.length);
        this.lastModified = lastModified;
    }

    @Override
    public String getFileName() {
        return fileName;
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
        return data.length;
    }

    @Override
    public InputStream doGetInputStream(boolean raw) {
        return new ByteArrayInputStream(data);
    }

    @Override
    public boolean isWritable() {
        return writable;
    }

    @Override
    public OutputStream doGetOutputStream() throws IOException {
        return new MemoryOutputStream();
    }

    @Override
    protected Collection<Resource> doList() {
        return Collections.emptyList();
    }

    @Override
    public Resource resolve(String path, Type type) {
        return Resource.NULL;
    }

    @Override
    public Resource get(String path, Type type) {
        return Resource.NULL;
    }

    @Override
    public URI toURI() {
        try {
            return new URI("memory://" + getId() + "/" + getFileName());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected Metrics getMetrics() {
        return METRICS;
    }

    @Override
    protected void updateHash(Hashing hashing) {
        hashing.update(data);
    }

    class MemoryOutputStream extends ByteArrayOutputStream {

        @Override
        public void close() throws IOException {
            super.close();

            data = toByteArray();
        }
    }
}