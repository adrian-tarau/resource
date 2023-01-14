package net.microfalx.resource.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import net.microfalx.resource.*;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import static net.microfalx.resource.ResourceUtils.addEndSlash;
import static net.microfalx.resource.ResourceUtils.requireNonNull;

public class S3Resource extends AbstractStatefulResource<AmazonS3Client, AmazonS3Client> {

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
        Type type = typeFromPath(path, null);
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
        return ResourceUtils.getFileName(uri.getPath());
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
        String _uri = addEndSlash(uri.toASCIIString()) + path;
        try {
            return S3Resource.create(URI.create(_uri), getCredential());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create a child resource for " + _uri, e);
        }
    }

    @Override
    public Resource resolve(String path, Type type) {
        requireNonNull(path);
        requireNonNull(type);
        String _uri = addEndSlash(uri.toASCIIString()) + path;
        try {
            return S3Resource.create(type, URI.create(_uri), getCredential());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create a child resource for " + _uri, e);
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
}
