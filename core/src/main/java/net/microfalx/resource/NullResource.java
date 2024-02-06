package net.microfalx.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * A resource for a "does not exist resource".
 */
public class NullResource extends AbstractResource {

    static final long serialVersionUID = 7347627536253212324L;

    /**
     * Creates a resource which do not exist and has no content.
     * <p>
     * Unless a new instance is needed (which should not be the case), it is recomented to use {@link Resource#NULL}.
     *
     * @return a non-null resource
     */
    public static Resource createNull() {
        String id = UUID.randomUUID().toString();
        return new NullResource(Type.FILE, id);
    }

    private NullResource(Type type, String id) {
        super(type, id);
    }

    @Override
    public String getFileName() {
        return getId();
    }

    @Override
    public InputStream doGetInputStream(boolean raw) {
        return new ByteArrayInputStream(new byte[0]);
    }

    @Override
    protected OutputStream doGetOutputStream() throws IOException {
        return new NoopOutputStream();
    }

    @Override
    public boolean doExists() {
        return false;
    }

    @Override
    protected long doLastModified() {
        return 0;
    }

    @Override
    protected long doLength() {
        return 0;
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
            return new URI("file:///dev/null");
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    static class NoopOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            // empty on purpose
        }
    }
}
