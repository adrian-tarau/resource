package net.tarau.resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import static net.tarau.resource.ResourceUtils.*;

/**
 * A resource implementation for a URL.
 */
public class UrlResource extends AbstractResource {

    private static final Logger LOGGER = Logger.getLogger(UrlResource.class.getName());

    private static final long serialVersionUID = -2384627536253212324L;

    private final String fileName;
    private final URL url;

    /**
     * Create a new resource from an URL and with a relative path to an arbitrary root.
     *
     * @param uri the URI of the resource
     * @return a non-null instance
     */
    public static Resource create(URI uri) throws IOException {
        return create(uri.toURL(), Type.FILE);
    }

    /**
     * Create a new resource from an URL and with a relative path to an arbitrary root.
     *
     * @param url the URL of the resource
     * @return a non-null instance
     */
    public static Resource create(URL url) {
        return create(url, Type.FILE);
    }

    /**
     * Create a new resource from an URL and with a relative path to an arbitrary root.
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

        this.fileName = ResourceUtils.getFileName(url.getPath());
        this.url = url;
    }

    @Override
    public Resource getParent() {
        String path = url.getPath();
        path = getParentPath(path);
        if (isEmpty(path)) {
            return null;
        }
        try {
            URL _url = new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
            return UrlResource.create(_url);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to create a parent URL for " + url.toExternalForm(), e);
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return url.openStream();
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public boolean exists() {
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
    public Collection<Resource> list() {
        String urlAsString = url.toExternalForm();
        try {
            URLConnection urlConnection = url.openConnection();
            if (urlConnection instanceof JarURLConnection) {
                JarFile jarFile = ((JarURLConnection) urlConnection).getJarFile();
                JarEntry rootJarEntry = ((JarURLConnection) urlConnection).getJarEntry();
                String rootName = rootJarEntry.getName();
                int rootNameDepth = split(rootName, "/").length;

                Collection<Resource> resources = new ArrayList<>();
                urlAsString = addEndSlash(urlAsString);
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement();
                    String name = jarEntry.getName();
                    Type type = Type.FILE;
                    if (jarEntry.isDirectory()) {
                        type = Type.DIRECTORY;
                    }
                    name = removeEndSlash(name);
                    int nameDepth = split(name, "/").length;
                    if (name.startsWith(rootName) && !name.equals(rootName) && (nameDepth == rootNameDepth + 1)) {
                        int index = urlAsString.lastIndexOf("!/");
                        String urlWithoutEntryPath = urlAsString.substring(0, index);
                        // only entries started with root and only next level
                        URL childUrl = new URL(urlWithoutEntryPath + "!/" + name);
                        resources.add(UrlResource.create(childUrl, type));
                    }
                }
                return resources;
            } else if (isFileUrl(url)) {
                File file = new File(url.getPath());
                File[] children = file.listFiles();
                if (isEmpty(children)) {
                    return Collections.emptyList();
                }
                urlAsString = addEndSlash(urlAsString);
                Collection<Resource> resources = new ArrayList<>();
                for (File child : children) {
                    Type type = Type.FILE;
                    if (child.isDirectory()) {
                        type = Type.DIRECTORY;
                    }
                    URL childUrl = new URL(urlAsString + child.getName());
                    resources.add(UrlResource.create(childUrl, type));
                }
                return resources;
            } else {
                throw new IllegalStateException("Unhandled protocol: " + url);
            }
        } catch (FileNotFoundException e) {
            // if we get this, there is no resource at this URL
            return Collections.emptyList();
        } catch (IOException e) {
            // we cannot read, presume is not there
            throw new IllegalStateException("Failed to list resource: " + url, e);
        }
    }

    @Override
    public Resource resolve(String path) {
        String _url = url.toExternalForm();
        if (!_url.endsWith("/")) {
            _url += "/";
        }
        _url += path;
        try {
            return UrlResource.create(new URL(_url));
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to create a new URL: " + _url, e);
        }
    }

    @Override
    public long lastModified() {
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
    public long length() {
        long size = 0;
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
    public URI toURI() {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("URL cannot be converted to URI: " + url.toExternalForm());
        }
    }

}
