package net.microfalx.resource;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * A factory used to create resources.
 */
public class ResourceFactory {

    private static Logger LOGGER = Logger.getLogger(ResourceFactory.class.getName());

    private static final List<ResourceResolver> resolvers = new CopyOnWriteArrayList<>();

    /**
     * Creates a resource from a URI.
     * <p>
     * If a provider does not exist, it will return a "NULL" resource.
     *
     * @param uri the URI
     * @return a non-null instance
     */
    public static Resource resolve(URI uri) {
        return resolve(uri, null);
    }

    /**
     * Creates a resource from a URI.
     * <p>
     * If a provider does not exist, it will return a "NULL" resource.
     *
     * @param uri        the URI
     * @param credential the credential, can be NULL
     * @return a non-null instance
     */
    public static Resource resolve(URI uri, Credential credential) {
        ResourceUtils.requireNonNull(uri);
        if (credential == null) credential = new NullCredential();
        initialize();
        for (ResourceResolver resolver : resolvers) {
            if (resolver.supports(uri)) {
                Resource resource = resolver.resolve(uri);
                if (resource instanceof AbstractResource) {
                    ((AbstractResource) resource).setCredential(credential);
                }
                return resource;
            }
        }
        return NullResource.createNull();
    }

    /**
     * Clears any caches use by providers.
     */
    public static void clearCache() {
        for (ResourceResolver resolver : resolvers) {
            resolver.clearCache();
        }
    }

    /**
     * Initializes the providers
     */
    private static void initialize() {
        if (!resolvers.isEmpty()) return;
        LOGGER.fine("Initialize resource resolvers");
        ServiceLoader<ResourceResolver> loader = ServiceLoader.load(ResourceResolver.class);
        for (ResourceResolver resolver : loader) {
            LOGGER.fine(" - " + resolver.getClass().getName());
            resolvers.add(resolver);
        }
        resolvers.sort(Comparator.comparingInt(ResourceResolver::getOrder).reversed());
    }
}
