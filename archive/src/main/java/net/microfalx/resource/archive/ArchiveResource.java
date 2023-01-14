package net.microfalx.resource.archive;

import net.microfalx.resource.AbstractResource;
import net.microfalx.resource.Resource;

import java.net.URI;

/**
 * A resource implementation on top of Apache Common Compress.
 */
public class ArchiveResource extends AbstractResource {

    public ArchiveResource(Type type, String id) {
        super(type, id);
    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public Resource resolve(String path) {
        return null;
    }

    @Override
    public URI toURI() {
        return null;
    }
}
