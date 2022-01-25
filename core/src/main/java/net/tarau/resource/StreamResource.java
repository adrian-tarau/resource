package net.tarau.resource;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import static net.tarau.resource.ResourceUtils.isEmpty;
import static net.tarau.resource.ResourceUtils.requireNonNull;

/**
 * A resource based on an {@link InputStream}.
 * <p>
 * The stream can be consumed once.
 */
public final class StreamResource extends AbstractResource {

    static final long serialVersionUID = 4387627536253212324L;

    private final InputStream inputStream;
    private final String fileName;
    private final URI uri;
    private final long lastModified = System.currentTimeMillis();
    private volatile boolean streamConsumed;

    /**
     * Creates a new resource.
     *
     * @param inputStream the input stream used as content for resource
     * @return a non-null instance
     */
    public static Resource create(InputStream inputStream) {
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
        return create(inputStream, fileName, null);
    }

    /**
     * Creates a new resource from a byte array.
     *
     * @param inputStream the input stream used as content for resource
     * @param fileName    the file name associated with the memory stream
     * @return a non-null instance
     */
    public static Resource create(InputStream inputStream, String fileName, URI uri) {
        requireNonNull(inputStream);
        requireNonNull(fileName);

        String id = UUID.randomUUID().toString();
        if (isEmpty(fileName)) {
            fileName = id;
        }
        return new StreamResource(inputStream, id, fileName, uri);
    }

    private StreamResource(InputStream inputStream, String id, String fileName, URI uri) {
        super(Type.FILE, id);

        this.inputStream = inputStream;
        this.fileName = fileName;
        this.uri = uri;
    }

    @Override
    public Resource getParent() {
        return null;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public InputStream doGetInputStream() throws IOException {
        if (streamConsumed) {
            throw new IOException("Stream '" + getFileName() + "' already consumed");
        }
        try {
            return inputStream;
        } finally {
            streamConsumed = true;
        }
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public long lastModified() {
        return lastModified;
    }

    @Override
    public long length() {
        return -1;
    }

    @Override
    public Collection<Resource> list() {
        return Collections.emptyList();
    }

    @Override
    public Resource resolve(String path) {
        return NullResource.createNull();
    }

    @Override
    public URI toURI() {
        if (uri != null) {
            return uri;
        }
        try {
            return new URI("memory://" + getId() + "/" + getFileName());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
