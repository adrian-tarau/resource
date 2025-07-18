package net.microfalx.resource;

import net.microfalx.lang.Hashing;
import net.microfalx.lang.StringUtils;
import net.microfalx.metrics.Metrics;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.ExceptionUtils.rethrowExceptionAndReturn;
import static net.microfalx.lang.StringUtils.isEmpty;
import static net.microfalx.lang.StringUtils.split;
import static net.microfalx.lang.ThreadUtils.sleepMillis;

/**
 * Various utilities around resources.
 */
public class ResourceUtils {

    public static final String CLASS_PATH_SCHEME = "classpath";
    public static final String FILE_SCHEME = "file";
    public static final String HTTP_SCHEME = "http";
    public static final String HTTPS_SCHEME = "https";
    public static final String SHARED = "shared";

    public static final String SUB_RESOURCE_SEPARATOR = "#";

    public static final int MAX_RETRY_COUNT = 3;
    public static final int MAX_SLEEP_BETWEEN_RETRIES = 10;
    public static final int MAX_NAME_LENGTH = 60;
    public static final int MAX_BODY_LENGTH = 10 * MAX_NAME_LENGTH;

    public static final byte[] EMPTY_BYTES = new byte[0];
    public static final String SLASH = "/";

    static NullCredential NULL_CREDENTIAL = new NullCredential();

    /**
     * Holds all metrics related to resource
     */
    public static Metrics METRICS = Metrics.of("Resource");

    /**
     * Returns whether the path points to a resource root.
     *
     * @param path the path
     * @return {@code true} if root, {@code false} otherwise
     */
    public static boolean isRoot(String path) {
        return StringUtils.isEmpty(path) || SLASH.equals(path);
    }

    /**
     * Returns whether the URL points to a local file.
     *
     * @param url the URL
     * @return <code>true</code> if a local file, <code>false</code> otherwise
     */
    public static boolean isFileUrl(URL url) {
        return url.getProtocol() == null || FILE_SCHEME.equalsIgnoreCase(url.getProtocol());
    }

    /**
     * Returns whether the URI points to a local file.
     *
     * @param uri the URI
     * @return <code>true</code> if a local file, <code>false</code> otherwise
     */
    public static boolean isFileUri(URI uri) {
        return uri.getScheme() == null || FILE_SCHEME.equalsIgnoreCase(uri.getScheme());
    }

    /**
     * Converts an URI which points to a local file to the scheme expected by the local file system.
     *
     * @param uri the original URI
     * @return the normalize URI
     */
    public static URI toFileUri(URI uri) {
        if (uri.getScheme().equalsIgnoreCase(FILE_SCHEME)) return uri;
        try {
            return new URI(FILE_SCHEME, uri.getAuthority(), uri.getPath(), null, null);
        } catch (URISyntaxException e) {
            throw new ResourceException("Failed to create a local file system URI from " + uri, e);
        }
    }

    /**
     * Loads this resource as a string.
     * <p>
     * The method throws an exception if the resource cannot be loaded.
     *
     * @param resource the resource to load
     * @return a non-null string
     */
    public static String loadAsString(Resource resource) {
        requireNonNull(resource);
        try {
            return resource.loadAsString();
        } catch (IOException e) {
            return rethrowExceptionAndReturn(e);
        }
    }

    /**
     * Returns a file which ends with {@link File#separator}to make it look like a directory.
     *
     * @param file the file
     * @return the changed file
     */
    public static File toDirectory(File file) {
        requireNonNull(file);
        String path = file.getAbsolutePath();
        if (!path.endsWith(File.separator)) path += File.separator;
        return new File(path);
    }

    /**
     * Returns a file path which ends with {@link File#separator} to make it look like a directory.
     *
     * @param path the uri
     * @return the changed uri
     */
    public static String toDirectory(String path) {
        requireNonNull(path);
        if (!path.endsWith(File.separator)) path += File.separator;
        return path;
    }

    /**
     * Returns a URI which ends with "/" to make it look like a directory.
     *
     * @param uri the uri
     * @return the changed uri
     */
    public static URI toDirectory(URI uri) {
        requireNonNull(uri);
        String path = uri.getPath();
        if (!path.endsWith(SLASH)) path += SLASH;
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), path, uri.getQuery(), uri.getFragment());
        } catch (URISyntaxException e) {
            return rethrowExceptionAndReturn(e);
        }
    }

    /**
     * Returns whether the URI looks like a directory (ends with "/").
     *
     * @param uri the URI (as a string) to check
     * @return {@code true} if looks like a directory, {@code false} otherwise
     */
    public static boolean isDirectory(String uri) {
        requireNonNull(uri);
        return uri.endsWith(SLASH);
    }

    /**
     * Returns the file behind a local resource.
     *
     * @param resource the resource
     * @return the file
     */
    public static File toFile(Resource resource) {
        return ((FileResource) resource.toFile()).getFile();
    }

    /**
     * Returns whether the resource exists.
     * <p>
     * If an exception is raised or the resource is null, it is considered that the resource does not exist.
     *
     * @param resource the resource (it can be NULL)
     * @return {@code true} if exists, {@code false} otherwise
     * @see Resource#exists()
     */
    public static boolean exists(Resource resource) {
        try {
            return resource != null && resource.exists();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Returns the string representation of the URI if the resource exists.
     *
     * @param resource the resource
     * @return the URI as string, null if the resource is null or it does not exists
     * @see Resource#exists()
     */
    public static String toUri(Resource resource) {
        return resource != null && exists(resource) ? resource.toURI().toASCIIString() : null;
    }

    /**
     * Creates a resource name out of a body of text.
     * <p>
     * Only first {@link #MAX_BODY_LENGTH} characters are considered and the name is limited truncated in the middle
     * at {@link #MAX_NAME_LENGTH} length.
     *
     * @param text the original text.
     * @return the name, N/A if the original text was empty
     */
    public static String createName(String text) {
        if (StringUtils.isEmpty(text)) return StringUtils.NA_STRING;
        if (text.length() > MAX_BODY_LENGTH) text = text.substring(0, MAX_BODY_LENGTH);
        return org.apache.commons.lang3.StringUtils.abbreviateMiddle(net.microfalx.lang.StringUtils.removeLineBreaks(text), "...", MAX_NAME_LENGTH);
    }

    /**
     * Normalizes a file system path to match the OS path separator.
     *
     * @param path the path
     * @return the normalize path
     */
    public static String normalizeFileSystemPath(String path) {
        requireNonNull(path);
        return path.replace('/', File.separatorChar);
    }

    /**
     * Returns whether the URI looks like a directory (ends with "/").
     *
     * @param uri the URI (as a string) to check
     * @return {@code true} if looks like a directory, {@code false} otherwise
     */
    public static boolean isDirectory(URI uri) {
        requireNonNull(uri);
        return uri.getPath().endsWith(SLASH);
    }

    /**
     * Returns whether the URL looks like a directory (ends with "/").
     *
     * @param url the URL (as a string) to check
     * @return {@code true} if looks like a directory, {@code false} otherwise
     */
    public static boolean isDirectory(URL url) {
        requireNonNull(url);
        return url.getPath().endsWith(SLASH);
    }

    /**
     * Compare whether two resources have the same content.
     * <p>
     * The resources are also considered to have the same content if they are both null references
     * (basically no content).
     *
     * @param firstResource  the first resource
     * @param secondResource the second resource
     * @return {@code true} if resources are the same, {@code false} otherwise
     * @throws IOException if the resource content cannot be processed
     */
    public static boolean hasSameContent(Resource firstResource, Resource secondResource) throws IOException {
        if (firstResource == null && secondResource == null) return true;
        if (!(firstResource != null && secondResource != null)) return false;
        String firstHash = Hashing.create().update(firstResource.getInputStream()).asString();
        String secondHash = Hashing.create().update(secondResource.getInputStream()).asString();
        return firstHash.equals(secondHash);
    }

    /**
     * Compares whether two resources have the same attributes: they exist, have the same length and optionally the same
     * last modified
     *
     * @param firstResource       the first resource
     * @param secondResource      the second resource
     * @param includeLastModified {@code true} to include last modified in comparison, {@code false} otherwise
     * @return {@code true} if resources have the same attributes, {@code false} otherwise
     * @throws IOException if an I/O error occurs
     */
    public static boolean hasSameAttributes(Resource firstResource, Resource secondResource, boolean includeLastModified) throws IOException {
        if (firstResource == null && secondResource == null) return true;
        if (firstResource == null || secondResource == null) return false;
        if (firstResource.exists() && secondResource.exists()) return true;
        if (firstResource.length() == secondResource.length()) return true;
        return includeLastModified && firstResource.lastModified() == secondResource.lastModified();
    }

    /**
     * Deletes the resource
     *
     * @param resource the resource to delete
     * @return {@code true} if the resource was deleted, {@code false} otherwise
     */
    public static boolean delete(Resource resource) {
        try {
            resource.delete();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Returns whether the file looks like a directory (ends with "/").
     *
     * @param file the URI (as a string) to check
     * @return {@code true} if looks like a directory, {@code false} otherwise
     */
    public static boolean isDirectory(File file, boolean useFileSystem) {
        requireNonNull(file);
        return file.getPath().endsWith(File.separator) || (useFileSystem && file.isDirectory());
    }

    /**
     * Calculates the type of the resource based on the path.
     *
     * @param path         the path, can be NULL
     * @param currentValue the current value, can be NULL
     * @return the resource type
     */
    public static Resource.Type getTypeFromPath(String path, Resource.Type currentValue) {
        if (isEmpty(path)) return Resource.Type.DIRECTORY;
        if (currentValue == null) currentValue = getTypeFromPath(path);
        return currentValue;
    }

    /**
     * Returns the resource type by looking at the path: if it ends with "/" it is presumed a directory,
     * otherwise a file.
     *
     * @param path the path
     * @return a non-ull type
     */
    public static Resource.Type getTypeFromPath(String path) {
        if (isEmpty(path)) return Resource.Type.DIRECTORY;
        return ResourceUtils.isDirectory(path) ? Resource.Type.DIRECTORY : Resource.Type.FILE;
    }

    /**
     * Appends a resource to the collection.
     * <p>
     * If the resource is a {@link CompositeResource}, it appends the children resources.
     *
     * @param resources              the resources
     * @param resource               the resource to add
     * @param deduplicateDirectories <code>true</code> to de-duplicate directories, <code>false</code> otherwise
     */
    public static void appendResource(Collection<Resource> resources, Resource resource, boolean deduplicateDirectories) {
        if (resource instanceof CompositeResource) {
            Collection<Resource> childResources = ((CompositeResource) resource).getResources();
            Set<Resource> added = new HashSet<>();
            if (deduplicateDirectories) {
                for (Resource childResource : childResources) {
                    if (childResource.isDirectory() && !added.add(resource)) continue;
                    resources.add(childResource);
                }
            } else {
                resources.addAll(childResources);
            }
        } else {
            resources.add(resource);
        }
    }

    /**
     * Throws an exception which indicates whether an operation is not supported.
     *
     * @param <T> a fake type
     * @return a fake value
     */
    public static <T> T throwUnsupported() {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Returns an URI from its string representation.
     * <p>
     * It handles Windows path formats
     *
     * @param uri the URI
     * @return the URI
     */
    public static URI toUri(String uri) {
        if (isEmpty(uri)) return null;
        if (uri.length() > 2 && Character.isAlphabetic(uri.charAt(0)) && uri.charAt(1) == ':') {
            uri = "file:///" + uri.replace('\\', '/');
        }
        return URI.create(uri);
    }

    /**
     * Calculates a hash out of a String.
     *
     * @param value the value
     * @return the hash
     */
    public static String hash(String value) {
        return Hashing.get(value);
    }

    /**
     * Returns the depth of a resource based on the number of sub-directory.
     *
     * @param path the path
     * @return the depth
     */
    public static int getDepth(String path) {
        if (path == null) return 0;
        path = path.replace('\\', '/');
        return split(path, "/").length;
    }

    /**
     * Performs an operation on behalf of a resource, with a maximum of N retries.
     *
     * @param resource the resource
     * @param callback the callback
     * @param <R>      the resource type
     * @return the result of the function
     */
    public static <R extends Resource> boolean retryWithStatus(R resource, Function<R, Boolean> callback) {
        int retryCount = MAX_RETRY_COUNT;
        while (retryCount-- > 0) {
            try {
                return callback.apply(resource);
            } catch (Exception e) {
                // we ignore and consider the result is false
            }
            sleepMillis(ThreadLocalRandom.current().nextInt(MAX_SLEEP_BETWEEN_RETRIES));
        }
        return false;
    }

    /**
     * Performs an operation on behalf of a resource, with a maximum of N retries.
     *
     * @param resource the resource
     * @param callback the callback
     * @param <T>      the result type
     * @return the result of the function
     */
    public static <T> T retryWithException(Resource resource, Function<Resource, T> callback) {
        Throwable throwable = null;
        int retryCount = MAX_RETRY_COUNT;
        while (retryCount-- > 0) {
            throwable = null;
            try {
                return callback.apply(resource);
            } catch (Exception e) {

                throwable = e;
            }
            sleepMillis(ThreadLocalRandom.current().nextInt(MAX_SLEEP_BETWEEN_RETRIES));
        }
        return rethrowExceptionAndReturn(throwable);
    }


}
