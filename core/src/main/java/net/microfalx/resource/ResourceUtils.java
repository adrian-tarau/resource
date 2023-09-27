package net.microfalx.resource;

import net.microfalx.lang.ExceptionUtils;
import net.microfalx.lang.Hashing;
import net.microfalx.metrics.Metrics;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
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

    public static final int MAX_RETRY_COUNT = 3;
    public static final int MAX_SLEEP_BETWEEN_RETRIES = 10;

    public static final byte[] EMPTY_BYTES = new byte[0];
    public static final String SLASH = "/";

    /**
     * Holds all metrics related to resource
     */
    protected static Metrics METRICS = Metrics.of("resource");

    /**
     * Returns whether the URL points to a local file.
     *
     * @param url the URL
     * @return <code>true</code> if a local file, <code>false</code> otherwise
     */
    public static boolean isFileUrl(URL url) {
        return url.getProtocol() == null || "file".equalsIgnoreCase(url.getProtocol());
    }

    /**
     * Returns whether the URI points to a local file.
     *
     * @param uri the URI
     * @return <code>true</code> if a local file, <code>false</code> otherwise
     */
    public static boolean isFileUri(URI uri) {
        return uri.getScheme() == null || "file".equalsIgnoreCase(uri.getScheme());
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
            return ExceptionUtils.throwException(e);
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
        return ExceptionUtils.throwException(throwable);
    }


}
