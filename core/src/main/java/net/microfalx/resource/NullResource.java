package net.microfalx.resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
    public Resource getParent() {
        return null;
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
    public Resource resolve(String path) {
        return NullResource.createNull();
    }

    @Override
    public URI toURI() {
        try {
            return new URI("file:///dev/null");
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
