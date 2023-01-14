package net.microfalx.resource.archive;

import net.microfalx.resource.Resource;
import net.microfalx.resource.ResourceResolver;

import java.net.URI;

public class ArchiveResourceResolver implements ResourceResolver {

    @Override
    public boolean supports(URI uri) {
        return false;
    }

    @Override
    public Resource resolve(URI uri) {
        return null;
    }
}
