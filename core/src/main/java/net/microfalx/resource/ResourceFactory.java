package net.microfalx.resource;

import net.microfalx.lang.AnnotationUtils;
import net.microfalx.lang.JvmUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.StringUtils.removeEndSlash;
import static net.microfalx.lang.StringUtils.removeStartSlash;
import static net.microfalx.resource.ResourceUtils.toUri;

/**
 * A factory used to create resources.
 */
public class ResourceFactory {

    private static Logger LOGGER = Logger.getLogger(ResourceFactory.class.getName());

    private static final List<ResourceResolver> resolvers = new CopyOnWriteArrayList<>();
    private static final List<ResourceProcessor> processors = new CopyOnWriteArrayList<>();
    private static final List<MimeTypeResolver> mimeTypeResolvers = new CopyOnWriteArrayList<>();
    private static final Map<String, Resource> links = new ConcurrentHashMap<>();

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
        return resolve(uri, null, null);
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
        return resolve(toUri(uri), credential, null);
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
        requireNonNull(root);
        if (root.getType() == Resource.Type.FILE) {
            throw new IllegalArgumentException("Only a directory can be the root of the shared resources, received '" + root.toURI() + "'");
        }
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
     * Returns registered mime type resolvers.
     *
     * @return a non-null instance
     */
    public static Collection<MimeTypeResolver> getMimeTypeResolvers() {
        initialize();
        return Collections.unmodifiableCollection(mimeTypeResolvers);
    }

    /**
     * Registers a symlink inside a shared resource which points to a different resource.
     * <p>
     * If a symlink is already registered for a given path, not change is made.
     *
     * @param path   the symlinked path
     * @param target the resource where the link delegates all the requests.
     */
    public static void registerSymlink(String path, Resource target) {
        requireNonNull(path);
        requireNonNull(target);
        links.putIfAbsent(removeStartSlash(removeEndSlash(path)), target);
    }

    /**
     * Resolves a symlink, if one matching the path is registered.
     *
     * @param path the requested path
     * @return the resource, null if the patch does not match a symlinked resource
     */
    static Resource resolveSymlink(String path, Resource.Type type) {
        if (links.isEmpty()) return null;
        String normalizedPath = removeEndSlash(removeStartSlash(path));
        String normalizedPathLowerCase = normalizedPath.toLowerCase();
        for (Map.Entry<String, Resource> entry : links.entrySet()) {
            if (normalizedPathLowerCase.startsWith(entry.getKey())) {
                path = removeStartSlash(normalizedPath.substring(entry.getKey().length()));
                return entry.getValue().resolve(path, type);
            }
        }
        return null;
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
        for (ResourceProcessor processor : getProcessors()) {
            inputStream = processor.getInputStream(resource, inputStream);
        }
        return inputStream;
    }

    /**
     * Returns the mime type for a given stream.
     * <p>
     * The method returns {@link MimeType#APPLICATION_OCTET_STREAM} if an existing mime type cannot be detected.
     *
     * @param inputStream the input stream
     * @param fileName    the file name
     * @return the mime type
     */
    public static String detect(InputStream inputStream, String fileName) {
        requireNonNull(fileName);
        requireNonNull(inputStream);

        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        for (MimeTypeResolver mimeTypeResolver : getMimeTypeResolvers()) {
            String mimeType = mimeTypeResolver.detect(bufferedInputStream, fileName);
            if (!MimeType.APPLICATION_OCTET_STREAM.equals(mimeType)) return mimeType;
        }
        return MimeType.APPLICATION_OCTET_STREAM.toString();
    }

    public static Logger getLOGGER() {
        return LOGGER;
    }

    /**
     * Creates a resource from a URI.
     * <p>
     * If a provider does not exist, it will return a "NULL" resource.
     *
     * @param uri        the URI
     * @param credential the credential, can be NULL
     * @param type       the resource type, can be NULL for auto-selection
     * @return a non-null instance
     */
    public static Resource resolve(URI uri, Credential credential, Resource.Type type) {
        requireNonNull(uri);
        if (credential == null) credential = Credential.NA;
        for (ResourceResolver resolver : getResolvers()) {
            if (resolver.supports(uri)) {
                Resource resource = resolver.resolve(uri, type);
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
        AnnotationUtils.sort(ResourceFactory.resolvers);

        LOGGER.fine("Initialize resource processors");
        ServiceLoader<ResourceProcessor> processors = ServiceLoader.load(ResourceProcessor.class);
        for (ResourceProcessor processor : processors) {
            LOGGER.fine(" - " + processor.getClass().getName());
            ResourceFactory.processors.add(processor);
        }
        AnnotationUtils.sort(ResourceFactory.processors);

        LOGGER.fine("Initialize mime type resolvers");
        ServiceLoader<MimeTypeResolver> mimeTypeResolvers = ServiceLoader.load(MimeTypeResolver.class);
        for (MimeTypeResolver mimeTypeResolver : mimeTypeResolvers) {
            LOGGER.fine(" - " + mimeTypeResolver.getClass().getName());
            ResourceFactory.mimeTypeResolvers.add(mimeTypeResolver);
        }
        AnnotationUtils.sort(ResourceFactory.mimeTypeResolvers);

        File shared = new File(JvmUtils.getHomeDirectory(), ".shared");
        setRoot(FileResource.directory(shared));
    }
}
