package net.microfalx.resource;

import net.microfalx.metrics.Metrics;

import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static net.microfalx.lang.ExceptionUtils.throwException;
import static net.microfalx.lang.StringUtils.isEmpty;
import static net.microfalx.lang.StringUtils.split;

/**
 * Various utilities around resources.
 */
public class ResourceUtils {

    public static final String CLASS_PATH_SCHEME = "classpath";
    public static final String FILE_SCHEME = "file";
    public static final String HTTP_SCHEME = "http";
    public static final String HTTPS_SCHEME = "https";

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

    public static String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (value != null) md.update(value.getBytes());
            byte[] data = md.digest();
            return longToId(data, 0) + longToId(data, 8);
        } catch (NoSuchAlgorithmException e) {
            return throwException(e);
        }
    }

    public static String longToId(byte[] data, int offset) {
        long value = data[offset++] + (long) data[offset++] << 8 + (long) data[offset++] << 16 + (long) data[offset++] << 24 + (long) data[offset++] << 32 + (long) data[offset++] << 40 + (long) data[offset++] << 48 + (long) data[offset++] << 56;
        return Long.toString(value, 26);
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


}
