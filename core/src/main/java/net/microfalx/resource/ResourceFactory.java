package net.microfalx.resource;

import net.microfalx.lang.AnnotationUtils;
import net.microfalx.lang.JvmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.unmodifiableCollection;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.StringUtils.removeEndSlash;
import static net.microfalx.lang.StringUtils.removeStartSlash;
import static net.microfalx.resource.ResourceUtils.toUri;

/**
 * A factory used to create resources.
 */
public class ResourceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceFactory.class.getName());

    private static final List<ResourceResolver> resolvers = new CopyOnWriteArrayList<>();
    private static final List<ResourceProcessor> processors = new CopyOnWriteArrayList<>();
    private static final List<MimeTypeResolver> mimeTypeResolvers = new CopyOnWriteArrayList<>();
    private static final Map<String, Resource> links = new ConcurrentHashMap<>();

    private static volatile Resource temporary;
    private static volatile Resource workspace;
    private static volatile Resource shared;

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
     * Returns the resource used for the shared resources.
     *
     * @return the resource, null if not set
     * @see SharedResource
     */
    public static Resource getShared() {
        return shared;
    }

    /**
     * Changes the resource used for the shared resources.
     *
     * @param shared the shared resource
     */
    public static void setShared(Resource shared) {
        requireNonNull(shared);
        if (shared.getType() == Resource.Type.FILE) {
            throw new IllegalArgumentException("Only a directory can be used for shared resources, received '" + shared.toURI() + "'");
        }
        if (ResourceFactory.workspace != null) {
            LOGGER.info("Change shared resources from '{}' to '{}'", ResourceFactory.shared.toURI(), shared.toURI());
        }
        ResourceFactory.shared = shared;
    }

    /**
     * Returns the resource used for the process workspace (data preserved between restarts).
     *
     * @return the resource, null if not set
     * @see SharedResource
     */
    public static Resource getWorkspace() {
        return workspace;
    }

    /**
     * Changes the resource used for the process workspace (data preserved between restarts).
     *
     * @param workspace the workspace
     */
    public static void setWorkspace(Resource workspace) {
        requireNonNull(workspace);
        if (workspace.getType() == Resource.Type.FILE) {
            throw new IllegalArgumentException("Only a directory can be used for workspace, received '" + workspace.toURI() + "'");
        }
        if (!workspace.isLocal()) {
            throw new IllegalArgumentException("The temporary directory need to be a local resource");
        }
        if (ResourceFactory.workspace != null) {
            LOGGER.info("Change workspace resources from '{}' to '{}'", ResourceFactory.workspace.toURI(), workspace.toURI());
        }
        ResourceFactory.workspace = workspace;
    }

    /**
     * Returns the resource used for the process temporary resources.
     *
     * @return the resource, null if not set
     * @see SharedResource
     */
    public static Resource getTemporary() {
        return temporary;
    }

    /**
     * Changes the resource used for the process temporary resources.
     *
     * @param temporary the workspace
     */
    public static void setTemporary(Resource temporary) {
        requireNonNull(temporary);
        if (temporary.getType() == Resource.Type.FILE) {
            throw new IllegalArgumentException("Only a directory can be used for workspace, received '" + workspace.toURI() + "'");
        }
        if (!temporary.isLocal()) {
            throw new IllegalArgumentException("The temporary directory need to be a local resource");
        }
        if (ResourceFactory.temporary != null) {
            LOGGER.info("Change temporary resources from '{}' to '{}'", ResourceFactory.temporary.toURI(), temporary.toURI());
        }
        ResourceFactory.temporary = temporary;
        JvmUtils.setTemporaryDirectory(((FileResource) temporary.toFile()).getFile());
    }

    /**
     * Returns registered resource resolvers.
     *
     * @return a non-null instance
     */
    public static Collection<ResourceResolver> getResolvers() {
        initialize();
        return unmodifiableCollection(resolvers);
    }

    /**
     * Returns registered resource processors.
     *
     * @return a non-null instance
     */
    public static Collection<ResourceProcessor> getProcessors() {
        initialize();
        return unmodifiableCollection(processors);
    }

    /**
     * Returns registered mime type resolvers.
     *
     * @return a non-null instance
     */
    public static Collection<MimeTypeResolver> getMimeTypeResolvers() {
        initialize();
        return unmodifiableCollection(mimeTypeResolvers);
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
     * Resolves a symlink if one matching the path is registered.
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
     * @throws IOException I/O exception
     */
    public static String detect(InputStream inputStream, String fileName) throws IOException {
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
     * Clears any cache used by providers.
     */
    public static void clearCache() {
        for (ResourceResolver resolver : resolvers) {
            resolver.clearCache();
        }
    }

    /**
     * Initializes the providers.
     */
    static void initialize() {
        if (!resolvers.isEmpty()) return;
        synchronized (ResourceFactory.class) {
            if (resolvers.isEmpty()) doInitialize();
        }
    }

    private static void doInitialize() {
        LOGGER.debug("Initialize resource resolvers:");
        ServiceLoader<ResourceResolver> resolvers = ServiceLoader.load(ResourceResolver.class);
        for (ResourceResolver resolver : resolvers) {
            LOGGER.debug(" - {}", resolver.getClass().getName());
            ResourceFactory.resolvers.add(resolver);
        }
        AnnotationUtils.sort(ResourceFactory.resolvers);
        LOGGER.debug("Initialized {} resource resolvers", ResourceFactory.resolvers.size());

        LOGGER.debug("Initialize resource processors");
        ServiceLoader<ResourceProcessor> processors = ServiceLoader.load(ResourceProcessor.class);
        for (ResourceProcessor processor : processors) {
            LOGGER.debug(" - {}", processor.getClass().getName());
            ResourceFactory.processors.add(processor);
        }
        AnnotationUtils.sort(ResourceFactory.processors);
        LOGGER.debug("Initialized {} resource processors", ResourceFactory.processors.size());

        LOGGER.debug("Initialize mime type resolvers");
        ServiceLoader<MimeTypeResolver> mimeTypeResolvers = ServiceLoader.load(MimeTypeResolver.class);
        for (MimeTypeResolver mimeTypeResolver : mimeTypeResolvers) {
            LOGGER.debug(" - {}", mimeTypeResolver.getClass().getName());
            ResourceFactory.mimeTypeResolvers.add(mimeTypeResolver);
        }
        AnnotationUtils.sort(ResourceFactory.mimeTypeResolvers);
        LOGGER.debug("Initialized {} mime type resolvers", ResourceFactory.mimeTypeResolvers.size());

        File shared = new File(JvmUtils.getHomeDirectory(), ".shared");
        setShared(FileResource.directory(shared));
        File workspace = new File(JvmUtils.getHomeDirectory(), ".workspace");
        setWorkspace(FileResource.directory(workspace));
        setTemporary(FileResource.directory(JvmUtils.getTemporaryDirectory()));
    }
}
