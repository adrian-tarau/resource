package net.tarau.resource;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * A factory used to create resources.
 */
public class ResourceFactory {

    private static Logger LOGGER = Logger.getLogger(ResourceFactory.class.getName());

    private static final Collection<ResourceResolver> resolvers = new CopyOnWriteArrayList<>();

    /**
     * Creates a resource from a URI.
     * <p>
     * If a provider does not exist, it will return a "NULL" resource.
     *
     * @param uri the URI
     * @return a non-null instance
     */
    public static Resource resolve(URI uri) {
        ResourceUtils.requireNonNull(uri);
        initialize();
        for (ResourceResolver resolver : resolvers) {
            if (resolver.supports(uri)) return resolver.resolve(uri);
        }
        return NullResource.create();
    }

    /**
     * Initializes the providers
     */
    private static void initialize() {
        if (!resolvers.isEmpty()) return;
        LOGGER.fine("Initialize resource resolvers");
        ServiceLoader<ResourceResolver> loader = ServiceLoader.load(ResourceResolver.class);
        Iterator<ResourceResolver> iterator = loader.iterator();
        for (ResourceResolver resolver : loader) {
            LOGGER.fine(" - " + resolver.getClass().getName());
            resolvers.add(resolver);
        }
    }
}
