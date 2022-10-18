package net.microfalx.resource.s3;

import java.net.URI;

import static net.microfalx.resource.ResourceUtils.requireNonNull;

public class S3Utilities {

    public static final String S3_SCHEME = "s3";
    public static final String S3_SECURE_SCHEME = "s3s";
    public static final String S3_SECURE_SCHEME2 = "s3+tls";

    /**
     * Returns whether the URI suggests to use TLS.
     *
     * @param uri the URI
     * @return <code>true</code> to use TLS, <code>false</code> otherwise
     */
    public static boolean isSecure(URI uri) {
        requireNonNull(uri);

        String scheme = uri.getScheme();
        return scheme != null && (scheme.equalsIgnoreCase(S3_SECURE_SCHEME) || scheme.equalsIgnoreCase(S3_SECURE_SCHEME2));
    }
}
