package net.microfalx.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

import static net.microfalx.resource.ResourceUtils.*;

/**
 * A resource for class path resources.
 */
public final class ClassPathResource extends UrlResource {

    private static final Logger LOGGER = Logger.getLogger(ClassPathResource.class.getName());
    private static final long serialVersionUID = 3867554477920857115L;

    private final String path;

    private static Metrics metrics = METRICS.withGroup("classpath");

    /**
     * Create a new file resource from a resource in the class path.
     *
     * @param path the path of the resource
     * @return a non-null instance
     */
    public static Resource file(String path) {
        return create(removeEndSlash(path), Type.FILE);
    }

    /**
     * Create a new director resource from a resource in the class path.
     *
     * @param path the path of the resource
     * @return a non-null instance
     */
    public static Resource directory(String path) {
        return create(addEndSlash(path), Type.DIRECTORY);
    }

    /**
     * Create a new resource from a resource in the class path.
     *
     * @param path the path of the resource
     * @return a non-null instance
     */
    public static Resource create(String path) {
        return create(path, null);
    }

    /**
     * Create a new resource from a resource in the class path.
     *
     * @param path the path of the resource
     * @return a non-null instance
     */
    public static Resource create(String path, Type type) {
        requireNonNull(path);
        try {
            path = removeStartSlash(path);
            Enumeration<URL> resourceUrls = ClassPathResource.class.getClassLoader().getResources(path);
            Collection<URL> urls = toCollection(resourceUrls);
            if (urls.isEmpty()) {
                return NullResource.createNull();
            } else if (urls.size() > 1) {
                if (type == Type.FILE) {
                    LOGGER.warning("A file class path resource was requested (" + path + ") but multiple resources were located (" + urls + ")");
                }
                type = typeFromPath(path, type);
                Collection<Resource> resources = new ArrayList<>();
                for (URL url : urls) {
                    String id = url.toExternalForm();
                    ClassPathResource resource = new ClassPathResource(type, id, url, path);
                    resources.add(resource);
                }
                return new CompositeResource(type, path, resources);
            } else {
                type = typeFromPath(path, type);
                URL url = urls.iterator().next();
                String id = hash(url.toExternalForm());
                return new ClassPathResource(type, id, url, path);
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to extract class path resources for " + path + ", root cause " + e.getMessage());
            return NullResource.createNull();
        }
    }

    public ClassPathResource(Type type, String id, URL url, String path) {
        super(type, id, url);

        this.path = path;
    }

    /**
     * Returns the resource path relative to the class loader.
     *
     * @return a non-null instance
     */
    public String getPath() {
        return path;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    protected Metrics getMetrics() {
        return metrics;
    }

    static Collection<URL> toCollection(Enumeration<URL> enumeration) {
        Collection<URL> urls = new ArrayList<>();
        while (enumeration.hasMoreElements()) {
            URL url = enumeration.nextElement();
            urls.add(url);
        }
        return urls;
    }

    static class CompositeResource extends AbstractResource {

        private static final long serialVersionUID = 3313044998127532888L;

        private final String path;
        private final Collection<Resource> resources;

        public CompositeResource(Type type, String path, Collection<Resource> resources) {
            super(type, UUID.randomUUID().toString());

            requireNonNull(path);
            requireNonNull(resources);

            this.path = removeEndSlash(path);
            this.resources = resources;
        }

        @Override
        public Resource getParent() {
            String path = getParentPath(this.path);
            if (isEmpty(path)) {
                return ClassPathResource.create("/");
            }
            return ClassPathResource.create(path);
        }

        @Override
        public String getFileName() {
            return ResourceUtils.getFileName(path);
        }

        @Override
        public InputStream doGetInputStream() {
            return throwContainerException();
        }

        @Override
        public boolean doExists() {
            return true;
        }

        @Override
        protected long doLastModified() throws IOException {
            long lastModified = Long.MIN_VALUE;
            for (Resource resource : resources) {
                lastModified = Math.max(lastModified, resource.lastModified());
            }
            return lastModified;
        }

        @Override
        protected long doLength() throws IOException {
            long length = 0;
            for (Resource resource : resources) {
                length += resource.length();
            }
            return length;
        }

        @Override
        protected Collection<Resource> doList() throws IOException {
            Collection<Resource> children = new ArrayList<>();
            for (Resource resource : resources) {
                children.addAll(resource.list());
            }
            return Collections.unmodifiableCollection(children);
        }

        @Override
        public Resource resolve(String path) {
            String fullPath = this.path + "/" + removeStartSlash(path);
            return ClassPathResource.create(fullPath);
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public URI toURI() {
            return getFirstResource().toURI();
        }

        private Resource getFirstResource() {
            return resources.iterator().next();
        }

        private <T> T throwContainerException() {
            throw new IllegalStateException("Resource 'classpath://" + path + " is a container");
        }
    }

    public static class ClassPathResourceResolver implements ResourceResolver {

        @Override
        public boolean supports(URI uri) {
            String scheme = uri.getScheme();
            return CLASS_PATH_SCHEME.equalsIgnoreCase(scheme);
        }

        @Override
        public Resource resolve(URI uri) {
            return ClassPathResource.create(uri.getPath());
        }
    }
}
