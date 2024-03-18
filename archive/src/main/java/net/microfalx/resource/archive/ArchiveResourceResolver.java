package net.microfalx.resource.archive;

import net.microfalx.lang.annotation.Order;
import net.microfalx.resource.Resource;
import net.microfalx.resource.ResourceFactory;
import net.microfalx.resource.ResourceResolver;

import java.net.URI;

@Order(Order.LOW)
public class ArchiveResourceResolver implements ResourceResolver {

    private static final ThreadLocal<Boolean> FORWARD = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Override
    public boolean supports(URI uri) {
        if (FORWARD.get()) return false;
        return ArchiveResource.fromExtension(uri) != ArchiveResource.Type.AUTO;
    }

    @Override
    public Resource resolve(URI uri, Resource.Type type) {
        FORWARD.set(Boolean.TRUE);
        try {
            Resource resource = ResourceFactory.resolve(uri);
            return ArchiveResource.create(resource).withFragment(uri.getFragment());
        } finally {
            FORWARD.remove();
        }
    }
}
