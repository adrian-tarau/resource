package net.microfalx.resource;

import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.resource.ResourceUtils.toUri;

/**
 * A factory used to create resources.
 */
public class ResourceFactory {

    private static Logger LOGGER = Logger.getLogger(ResourceFactory.class.getName());

    private static final List<ResourceResolver> resolvers = new CopyOnWriteArrayList<>();
    private static final List<ResourceProcessor> processors = new CopyOnWriteArrayList<>();

    private static volatile Resource root;

    /**
     * Creates a resource from a URI.
     * <p>
     * If a provider does not exist, it will return a "NULL" resource.
     *
     * @param uri the URI in string form
     * @return a non-null instance
     */
    public static Resource resolve(String uri) {
        return resolve(uri, null);
    }

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
     * @param uri        the URI in string form
     * @param credential the credential, can be NULL
     * @return a non-null instance
     */
    public static Resource resolve(String uri, Credential credential) {
        requireNonNull(uri);
        return resolve(toUri(uri), credential);
    }

    /**
     * Returns the root resource used for the shared resources.
     *
     * @return the resource, null if not set
     * @see SharedResource
     */
    public static Resource getRoot() {
        return root;
    }

    /**
     * Changes the root resource used for the shared resources.
     *
     * @param root the root
     */
    public static void setRoot(Resource root) {
        ResourceFactory.root = root;
    }

    /**
     * Returns registered resource resolvers.
     *
     * @return a non-null instance
     */
    public static Collection<ResourceResolver> getResolvers() {
        initialize();
        return Collections.unmodifiableCollection(resolvers);
    }

    /**
     * Returns registered resource processors.
     *
     * @return a non-null instance
     */
    public static Collection<ResourceProcessor> getProcessors() {
        initialize();
        return Collections.unmodifiableCollection(processors);
    }

    /**
     * Intercepts the input stream of a resource.
     *
     * @param resource    the resource
     * @param inputStream the input stream
     * @return the same input stream or a processed input stream
     */
    public static InputStream process(Resource resource, InputStream inputStream) {
        requireNonNull(resource);
        requireNonNull(inputStream);
        for (ResourceProcessor processor : processors) {
            inputStream = processor.getInputStream(resource, inputStream);
        }
        return inputStream;
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
        requireNonNull(uri);
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
    static void initialize() {
        if (!resolvers.isEmpty()) return;

        LOGGER.fine("Initialize resource resolvers");
        ServiceLoader<ResourceResolver> resolvers = ServiceLoader.load(ResourceResolver.class);
        for (ResourceResolver resolver : resolvers) {
            LOGGER.fine(" - " + resolver.getClass().getName());
            ResourceFactory.resolvers.add(resolver);
        }
        ResourceFactory.resolvers.sort(Comparator.comparingInt(ResourceResolver::getOrder).reversed());

        LOGGER.fine("Initialize resource processors");
        ServiceLoader<ResourceProcessor> processors = ServiceLoader.load(ResourceProcessor.class);
        for (ResourceProcessor processor : processors) {
            LOGGER.fine(" - " + processor.getClass().getName());
            ResourceFactory.processors.add(processor);
        }
        ResourceFactory.processors.sort(Comparator.comparingInt(ResourceProcessor::getOrder).reversed());
    }
}
