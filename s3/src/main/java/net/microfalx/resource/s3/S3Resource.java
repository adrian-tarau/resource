package net.microfalx.resource.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import net.microfalx.lang.FileUtils;
import net.microfalx.metrics.Metrics;
import net.microfalx.resource.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.StringUtils.addEndSlash;
import static net.microfalx.resource.ResourceUtils.getTypeFromPath;
import static net.microfalx.resource.s3.S3Utilities.S3_SCHEME;
import static net.microfalx.resource.s3.S3Utilities.S3_SECURE_SCHEME2;

public class S3Resource extends AbstractStatefulResource<AmazonS3Client, AmazonS3Client> {

    private static final Metrics METRICS = ResourceUtils.METRICS.withGroup("S3");

    private final URI uri;

    /**
     * Creates a resource with a file type.
     *
     * @param uri        the URI
     * @param credential the credential
     * @return a non-null instance
     */
    public static StatefulResource file(URI uri, Credential credential) {
        return create(Type.FILE, uri, credential);
    }

    /**
     * Creates a resource with a directory type.
     *
     * @param uri        the URI
     * @param credential the credential
     * @return a non-null instance
     */
    public static StatefulResource directory(URI uri, Credential credential) {
        return create(Type.DIRECTORY, uri, credential);
    }

    /**
     * Creates a resource and auto-detects the type.
     *
     * @param uri        the URI
     * @param credential the credential
     * @return a non-null instance
     */
    public static StatefulResource create(URI uri, Credential credential) {
        requireNonNull(uri);
        String path = uri.getPath();
        String id = ResourceUtils.hash(uri.toASCIIString());
        Type type = getTypeFromPath(path, null);
        return create(type, uri, credential);
    }

    /**
     * Creates a resource and auto-detects the type.
     *
     * @param type       the resource type
     * @param uri        the URI
     * @param credential the credential
     * @return a non-null instance
     */
    public static StatefulResource create(Type type, URI uri, Credential credential) {
        requireNonNull(type);
        requireNonNull(uri);
        requireNonNull(credential);
        String id = ResourceUtils.hash(uri.toASCIIString());
        S3Resource resource = new S3Resource(type, id, uri);
        resource.setCredential(credential);
        return resource;
    }

    private S3Resource(Type type, String id, URI uri) {
        super(type, id);
        requireNonNull(uri);
        this.uri = uri;
        setAbsolutePath(false);
    }

    @Override
    public String getFileName() {
        return FileUtils.getFileName(uri.getPath());
    }

    @Override
    public boolean doExists() throws IOException {
        return false;
    }

    @Override
    protected long doLastModified() throws IOException {
        return 0;
    }

    @Override
    protected long doLength() throws IOException {
        return 0;
    }

    @Override
    protected Collection<Resource> doList() throws IOException {
        return Collections.emptyList();
    }

    @Override
    public Resource resolve(String path) {
        requireNonNull(path);
        Type type = getTypeFromPath(path);
        return resolve(path, type);
    }

    @Override
    public Resource resolve(String path, Type type) {
        requireNonNull(path);
        requireNonNull(type);
        String newUri = addEndSlash(uri.toASCIIString()) + path;
        return createFromUri(newUri, type);
    }

    @Override
    public Resource get(String path, Type type) {
        requireNonNull(path);
        requireNonNull(type);
        try {
            URI newUri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), path, null, null);
            return createFromUri(newUri.toASCIIString(), type);
        } catch (URISyntaxException e) {
            throw new ResourceException("Invalid resource URL for path '" + path + "', original URI '" + uri + "'", e);
        }
    }

    @Override
    public URI toURI() {
        return uri;
    }

    @Override
    protected AmazonS3Client doCreateSession() throws Exception {
        S3ClientOptions clientOptions = S3ClientOptions
                .builder()
                .setPathStyleAccess(true)
                .build();

        Credential credential = getCredential();
        AWSCredentials awsCredentials;
        if (credential instanceof UserPasswordCredential) {
            UserPasswordCredential userPasswordCredential = (UserPasswordCredential) credential;
            awsCredentials = new BasicAWSCredentials(userPasswordCredential.getUserName(), userPasswordCredential.getPassword());
        } else {
            throw new IllegalArgumentException("Unexpected credential type " + credential.getClass().getName());
        }

        AmazonS3Client client = new AmazonS3Client(new AWSStaticCredentialsProvider(awsCredentials));
        client.setEndpoint(createEndpoint());
        client.setS3ClientOptions(clientOptions);
        return client;
    }

    @Override
    protected void doReleaseSession(AmazonS3Client session) throws Exception {
        // nothing to release
    }

    @Override
    protected boolean isValid(AmazonS3Client session) throws Exception {
        return false;
    }

    @Override
    protected AmazonS3Client doCreateChannel(AmazonS3Client session) throws Exception {
        return session;
    }

    @Override
    protected void doReleaseChannel(AmazonS3Client session, AmazonS3Client channel) throws Exception {
        // nothing to release
    }

    @Override
    protected IOException translateException(Exception e) {
        return null;
    }

    @Override
    protected Metrics getMetrics() {
        return METRICS;
    }

    /**
     * Executes a command with a client.
     *
     * @param callback the callback
     * @param <R>      the return type
     * @return the value returned by the callback
     * @throws IOException if an I/O error occurs
     */
    private <R> R doWithChannel(ChannelCallback<AmazonS3Client, R> callback) throws IOException {
        requireNonNull(callback);
        AmazonS3Client session = createSession();
        AmazonS3Client channel = null;
        try {
            channel = createChannel(session);
            return callback.doWithChannel(channel);
        } catch (Exception e) {
            throw translateException(e);
        } finally {
            releaseChannel(session, channel);
        }
    }

    private String createEndpoint() {
        StringBuffer endPoint = new StringBuffer();
        endPoint.append(S3Utilities.isSecure(uri) ? "https" : "http");
        endPoint.append("://").append(uri.getHost());
        if (uri.getPort() > 0) endPoint.append(uri.getPath());
        endPoint.append(uri.getPath());
        return endPoint.toString();
    }


    /**
     * Creates a new resource from a URI and a type, using the same credentials as this resource.
     *
     * @param uri  the URI as string
     * @param type the type
     * @return a new instance
     */
    private Resource createFromUri(String uri, Type type) {
        return S3Resource.create(type, URI.create(uri), getCredential());
    }

    public static class S3ResourceResolver implements ResourceResolver {

        @Override
        public boolean supports(URI uri) {
            return S3_SCHEME.equalsIgnoreCase(uri.getScheme()) || S3_SECURE_SCHEME2.equalsIgnoreCase(uri.getScheme());
        }

        @Override
        public Resource resolve(URI uri, Type type) {
            return S3Resource.create(type, uri, Credential.NA).withFragment(uri.getFragment());
        }
    }
}
