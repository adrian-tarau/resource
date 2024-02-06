package net.microfalx.resource;

import net.microfalx.lang.FileUtils;
import net.microfalx.lang.ObjectUtils;
import net.microfalx.metrics.Metrics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import static java.util.Collections.emptyList;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.FileUtils.getParentPath;
import static net.microfalx.lang.IOUtils.closeQuietly;
import static net.microfalx.lang.StringUtils.*;
import static net.microfalx.resource.ResourceUtils.*;

/**
 * A resource implementation for a URL.
 */
public class UrlResource extends AbstractResource {

    private static final Logger LOGGER = Logger.getLogger(UrlResource.class.getName());

    private static final long serialVersionUID = -2384627536253212324L;

    private final URL url;
    private Metrics metrics;

    /**
     * Create a new resource from a URL and with a relative path to an arbitrary root.
     *
     * @param uri the URI of the resource
     * @return a non-null instance
     */
    public static Resource create(URI uri) throws IOException {
        Type type = ResourceUtils.isDirectory(uri) ? Type.DIRECTORY : Type.FILE;
        return create(uri.toURL(), type);
    }

    /**
     * Create a new resource from a URL and with a relative path to an arbitrary root.
     *
     * @param url the URL of the resource
     * @return a non-null instance
     */
    public static Resource create(URL url) {
        Type type = ResourceUtils.isDirectory(url) ? Type.DIRECTORY : Type.FILE;
        return create(url, type);
    }

    /**
     * Create a new resource from a URL and with a relative path to an arbitrary root.
     *
     * @param url the URL of the resource
     * @return a non-null instance
     */
    public static Resource create(URL url, Type type) {
        requireNonNull(url);
        String id = hash(url.toExternalForm());
        return new UrlResource(type, id, url);
    }

    protected UrlResource(Type type, String id, URL url) {
        super(type, id);
        this.url = url;
    }

    @Override
    public Resource getParent() {
        String path = url.getPath();
        path = getParentPath(path);
        if (isEmpty(path)) return null;
        try {
            URL _url = new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
            return UrlResource.create(_url, Type.DIRECTORY);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to create a parent URL for " + url.toExternalForm(), e);
        }
    }

    @Override
    public final InputStream doGetInputStream(boolean raw) throws IOException {
        return url.openStream();
    }

    @Override
    public final String getFileName() {
        return FileUtils.getFileName(url.getPath());
    }

    @Override
    public final boolean doExists() {
        try {
            InputStream inputStream = url.openStream();
            closeQuietly(inputStream);
            return true;
        } catch (IOException e) {
            LOGGER.fine("Resource does not exist: " + url.toExternalForm());
        }
        return false;
    }

    @Override
    protected final Collection<Resource> doList() {
        try {
            URLConnection urlConnection = url.openConnection();
            if (urlConnection instanceof JarURLConnection) {
                JarFile jarFile = ((JarURLConnection) urlConnection).getJarFile();
                JarEntry rootJarEntry = ((JarURLConnection) urlConnection).getJarEntry();
                String rootName = rootJarEntry.getName();
                int rootNameDepth = split(rootName, "/").length;
                Collection<Resource> resources = new ArrayList<>();
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement();
                    String name = jarEntry.getName();
                    Type type = jarEntry.isDirectory() ? Type.DIRECTORY : Type.FILE;
                    name = removeEndSlash(name);
                    int nameDepth = split(name, "/").length;
                    if (name.startsWith(rootName) && !name.equals(rootName) && (nameDepth == rootNameDepth + 1)) {
                        appendResource(resources, get(name, type), true);
                    }
                }
                return resources;
            } else if (isFileUrl(url)) {
                File file = new File(url.getPath());
                File[] children = file.listFiles();
                if (ObjectUtils.isEmpty(children)) return emptyList();
                Collection<Resource> resources = new ArrayList<>();
                for (File child : children) {
                    Type type = child.isDirectory() ? Type.DIRECTORY : Type.FILE;
                    appendResource(resources, resolve(child.getName(), type), true);
                }
                return resources;
            } else {
                throw new IllegalStateException("Unhandled protocol: " + url);
            }
        } catch (FileNotFoundException e) {
            // if we get this, there is no resource at this URL
            return emptyList();
        } catch (IOException e) {
            // we cannot read, presume is not there
            throw new IllegalStateException("Failed to list resource " + url, e);
        }
    }

    @Override
    public Resource resolve(String path) {
        requireNonNull(path);
        Type type = getTypeFromPath(path);
        return createFromUriString(addEndSlash(url.toExternalForm()) + path, type);
    }

    @Override
    public Resource resolve(String path, Type type) {
        requireNonNull(path);
        requireNonNull(type);
        return createFromUriString(addEndSlash(url.toExternalForm()) + path, type);
    }

    @Override
    public Resource get(String path, Type type) {
        requireNonNull(path);
        requireNonNull(type);
        try {
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), path, null, null);
            return createFromUriString(uri.toASCIIString(), type);
        } catch (URISyntaxException e) {
            throw new ResourceException("Invalid resource URL for path '" + path + "', original URL '" + url + "'", e);
        }
    }

    @Override
    protected final long doLastModified() {
        long lastModified = -1;
        try {
            URLConnection urlConnection = url.openConnection();
            lastModified = urlConnection.getLastModified();
        } catch (IOException e) {
            // we cannot read, presume is not there
            LOGGER.fine("Cannot extract last modified for " + url + ", root cause " + e.getMessage());
        }
        return lastModified;
    }

    @Override
    protected final long doLength() {
        long size = -1;
        try {
            URLConnection urlConnection = url.openConnection();
            size = urlConnection.getContentLength();
        } catch (IOException e) {
            // we cannot read, presume is not there
            LOGGER.fine("Cannot extract length for " + url + ", root cause " + e.getMessage());
        }
        return size;
    }

    @Override
    public final URI toURI() {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("URL cannot be converted to URI: " + url.toExternalForm());
        }
    }

    @Override
    protected Metrics getMetrics() {
        if (metrics == null) metrics = METRICS.withTag("host", url.getHost());
        return metrics;
    }

    /**
     * Creates a new resource from a URL and a type.
     *
     * @param url  the URL as string
     * @param type the type
     * @return a new instance
     */
    protected Resource createFromUriString(String url, Type type) {
        try {
            return UrlResource.create(URI.create(url).toURL(), type);
        } catch (MalformedURLException e) {
            throw new ResourceException("Invalid resource URL for '" + url + "'", e);
        }
    }
}
