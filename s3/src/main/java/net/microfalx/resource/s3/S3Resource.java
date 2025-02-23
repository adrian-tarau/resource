package net.microfalx.resource.s3;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import io.minio.messages.Owner;
import net.microfalx.lang.*;
import net.microfalx.metrics.Metrics;
import net.microfalx.resource.*;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.StringUtils.*;
import static net.microfalx.lang.TimeUtils.ONE_MINUTE;
import static net.microfalx.lang.TimeUtils.millisSince;
import static net.microfalx.lang.UriUtils.SLASH;
import static net.microfalx.resource.ResourceUtils.getTypeFromPath;
import static net.microfalx.resource.s3.S3Utilities.S3_SCHEME;
import static net.microfalx.resource.s3.S3Utilities.S3_SECURE_SCHEME2;

public class S3Resource extends AbstractStatefulResource<MinioClient, MinioClient> {

    private static final Metrics METRICS = ResourceUtils.METRICS.withGroup("S3");

    private static volatile URL defaultEndpoint = UriUtils.parseUrl("https://s3.amazonaws.com");

    private URL endpoint;
    private final URI uri;

    private volatile Boolean bucketExists;
    private volatile StatObjectResponse stats;
    private volatile long lastStatsUpdate;
    private volatile Long lastModified;
    private volatile Long size;
    private volatile String etag;
    private volatile String owner;

    private static final ThreadLocal<Boolean> UPLOADING = ThreadLocal.withInitial(() -> Boolean.FALSE);

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

    /**
     * Changes the default endpoint.
     *
     * @param url the URL of the S3 object store
     */
    public static void setDefaultEndpoint(URL url) {
        requireNonNull(url);
        defaultEndpoint = url;
    }

    /**
     * Returns the default endpoint.
     *
     * @return the endpoint, NULL if a default one is not provided
     */
    public static URL getDefaultEndpoint() {
        return defaultEndpoint;
    }

    private S3Resource(Type type, String id, URI uri) {
        super(type, id);
        requireNonNull(uri);
        this.uri = uri;
        if (StringUtils.isEmpty(uri.getPath())) {
            throw new S3Exception("The URI path must contain at least the bucket");
        }
        setAbsolutePath(false);
    }

    /**
     * Returns the URL to the S3 object store.
     *
     * @return a non-null instance
     * @throws S3Exception if the URL cannot be retrieved
     */
    public URL getEndpoint() {
        if (this.endpoint == null) {
            Object value = getAttribute(END_POINT_ATTR);
            if (ObjectUtils.isEmpty(value)) value = defaultEndpoint;
            if (ObjectUtils.isEmpty(value)) {
                throw new S3Exception("The endpoint was not provided and the default end point was not set");
            }
            this.endpoint = toUrl(value);
        }
        return endpoint;
    }

    @Override
    public String getFileName() {
        return FileUtils.getFileName(uri.getPath());
    }

    /**
     * Returns the ETag attached to the object.
     *
     * @return the ETag, null if not available
     */
    public String getEtag() throws IOException {
        if (etag != null && !areStatsStale()) return etag;
        StatObjectResponse currentStats = getStats();
        return currentStats != null ? currentStats.etag() : null;
    }

    /**
     * Returns the owner attached to the object.
     *
     * @return the owner, null if not available
     */
    public String getOwner() throws IOException {
        return owner;
    }

    @Override
    public boolean doExists() throws IOException {
        checkBucket();
        if (isEmpty(getObjectPath())) return bucketExists;
        StatObjectResponse currentStats = getStats();
        return currentStats != null && currentStats.lastModified() != null;
    }

    @Override
    protected long doLastModified() throws IOException {
        if (lastModified != null && !areStatsStale()) return lastModified;
        StatObjectResponse currentStats = getStats();
        return currentStats != null && currentStats.lastModified() != null ? TimeUtils.toMillis(currentStats.lastModified()) : 0;
    }

    @Override
    protected long doLength() throws IOException {
        if (size != null && !areStatsStale()) return size;
        StatObjectResponse currentStats = getStats();
        return currentStats != null && currentStats.lastModified() != null ? currentStats.size() : 0;
    }

    @Override
    protected String doGetMimeType() throws IOException {
        if (UPLOADING.get()) {
            return super.doGetMimeType();
        } else {
            StatObjectResponse currentStats = getStats();
            return MimeType.get(currentStats.contentType()).getValue();
        }
    }

    @Override
    protected void doCreate() throws IOException {
        // does not apply to S3
    }

    @Override
    protected void doCreateParents() throws IOException {
        // does not apply to S3
    }

    @Override
    protected void doDelete() throws IOException {
        // we cannot remove the bucket itself
        if (StringUtils.isEmpty(getObjectPath())) return;
        doWithChannel("delete", channel -> {
            try {
                RemoveObjectArgs args = RemoveObjectArgs.builder().bucket(getBucketName()).object(getObjectPath()).build();
                channel.removeObject(args);
                clearCache();
            } catch (ErrorResponseException e) {
                if (isMissingError(e)) return null;
                throw new S3Exception(getErrorMessage("Failed to delete object stats from ''{0}'' for ''{1}''"), e);
            }
            return null;
        });
    }

    @Override
    protected InputStream doGetInputStream(boolean raw) throws IOException {
        return doWithChannel("download", channel -> {
            GetObjectArgs args = GetObjectArgs.builder().bucket(getBucketName()).object(getObjectPath()).build();
            return channel.getObject(args);
        });
    }

    @Override
    protected OutputStream doGetOutputStream() throws IOException {
        File file = JvmUtils.getTemporaryFile("s3.", ".object");
        OutputStream delegate = IOUtils.getBufferedOutputStream(file);
        return new S3OutputStream(file, delegate);
    }

    @Override
    protected Collection<Resource> doList() throws IOException {
        return doWithChannel("list", channel -> {
            ListObjectsArgs args = ListObjectsArgs.builder().bucket(getBucketName()).prefix(getPrefix())
                    .delimiter(SLASH).build();
            Iterable<Result<Item>> results = channel.listObjects(args);
            return CollectionUtils.toList(new ItemIterator(this, results.iterator()));
        });
    }

    @Override
    protected boolean doWalk(ResourceVisitor visitor, int maxDepth) throws IOException {
        return doWithChannel("walk", channel -> {
            ListObjectsArgs args = ListObjectsArgs.builder().bucket(getBucketName()).prefix(getPrefix())
                    .delimiter(SLASH).recursive(true).build();
            Iterable<Result<Item>> results = channel.listObjects(args);
            ItemIterator iterator = new ItemIterator(this, results.iterator());
            boolean loop = true;
            while (iterator.hasNext() && loop) {
                Resource child = iterator.next();
                String path = child.getPath(this);
                if (split(path, SLASH).length <= maxDepth) {
                    loop = visitor.onResource(this, child);
                }
            }
            return loop;
        });
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
    protected MinioClient doCreateSession() throws Exception {
        MinioClient.Builder builder = MinioClient.builder().endpoint(getEndpoint());
        Credential credential = getCredential();
        if (credential instanceof UserPasswordCredential) {
            UserPasswordCredential userPasswordCredential = (UserPasswordCredential) credential;
            builder.credentials(userPasswordCredential.getUserName(), userPasswordCredential.getPassword());
        } else {
            throw new IllegalArgumentException("Unexpected credential type " + credential.getClass().getName());
        }
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder().callTimeout(Duration.ofSeconds(5))
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30));
        builder.httpClient(clientBuilder.build(), true);
        return builder.build();
    }

    @Override
    protected void doReleaseSession(MinioClient session) throws Exception {
        session.close();
    }

    @Override
    protected boolean isValid(MinioClient session) throws Exception {
        return true;
    }

    @Override
    protected MinioClient doCreateChannel(MinioClient session) throws Exception {
        return session;
    }

    @Override
    protected void doReleaseChannel(MinioClient session, MinioClient channel) throws Exception {
        // nothing to release for the channel
    }

    @Override
    protected IOException translateException(Exception e) {
        return new IOException("Failed to perform S3 operation", e);
    }

    @Override
    protected Metrics getMetrics() {
        return METRICS;
    }


    /**
     * Creates a new resource from a URI and a type, using the same credentials as this resource.
     *
     * @param uri  the URI as string
     * @param type the type
     * @return a new instance
     */
    private Resource createFromUri(String uri, Type type) {
        S3Resource resource = (S3Resource) S3Resource.create(type, URI.create(uri), getCredential());
        resource.bucketExists = bucketExists;
        resource.endpoint = endpoint;
        return resource;
    }

    private void uploadFile(File file) throws IOException {
        doWithChannel("upload", channel -> {
            UPLOADING.set(true);
            try {
                UploadObjectArgs.Builder builder = UploadObjectArgs.builder().bucket(getBucketName()).object(getObjectPath())
                        .filename(file.getAbsolutePath()).contentType(getMimeType());
                channel.uploadObject(builder.build());
            } finally {
                UPLOADING.remove();
            }
            return null;
        });
        clearCache();
    }

    private S3Resource update(ZonedDateTime lastModified, Long size, String etag, String owner) {
        this.etag = etag;
        this.owner = owner;
        this.size = size;
        this.lastModified = lastModified != null ? TimeUtils.toMillis(lastModified) : null;
        return this;
    }

    private String getBucketName() {
        if (StringUtils.isEmpty(getPath())) return null;
        return split(getPath(), SLASH)[0];
    }

    private String getObjectPath() {
        String[] parts = split(getPath(), SLASH);
        if (parts.length < 2) return null;
        parts = Arrays.copyOfRange(parts, 1, parts.length);
        return String.join(SLASH, parts);
    }

    private StatObjectResponse getStats() throws IOException {
        if (stats == null || areStatsStale()) {
            stats = doWithChannel("stats", channel -> {
                try {
                    StatObjectArgs args = StatObjectArgs.builder().bucket(getBucketName()).object(getObjectPath()).build();
                    return channel.statObject(args);
                } catch (ErrorResponseException e) {
                    if (isMissingError(e)) return null;
                    throw new S3Exception(getErrorMessage("Failed to retrieve object stats from ''{0}'' for ''{1}''"), e);
                }
            });
            if (stats != null && isNotEmpty(stats.contentType())) {
                setContentType(stats.contentType());
            }
            lastStatsUpdate = System.currentTimeMillis();
        }
        return stats;
    }

    private static boolean isMissingError(ErrorResponseException e) {
        String code = e.errorResponse().code();
        return NO_SUCH_KEY.equals(code) || NO_SUCH_BUCKET.equals(code);
    }

    private String getErrorMessage(String format) {
        return StringUtils.formatMessage(format, getEndpoint(), toURI().getPath());
    }

    private boolean areStatsStale() {
        return millisSince(lastStatsUpdate) > ONE_MINUTE;
    }

    private void checkBucket() throws IOException {
        if (bucketExists == null) {
            boolean found = doWithChannel("bucket exists", channel -> {
                BucketExistsArgs args = BucketExistsArgs.builder().bucket(getBucketName()).build();
                return channel.bucketExists(args);
            });
            if (!found) {
                doWithChannel("make bucket", channel -> {
                    MakeBucketArgs args = MakeBucketArgs.builder().bucket(getBucketName()).build();
                    channel.makeBucket(args);
                    return null;
                });
            }
            bucketExists = true;
        }
    }

    private URL toUrl(Object value) {
        if (value instanceof URL) {
            return (URL) value;
        } else if (value instanceof URI) {
            try {
                return ((URI) value).toURL();
            } catch (MalformedURLException e) {
                return ExceptionUtils.throwException(e);
            }
        } else if (value instanceof String) {
            return toUrl(URI.create((String) value));
        } else {
            throw new IllegalArgumentException("Unknown endpoint type: " + ClassUtils.getName(value));
        }
    }

    private String getPrefix() {
        String objectPath = getObjectPath();
        return StringUtils.isNotEmpty(objectPath) ? addEndSlash(objectPath) : EMPTY_STRING;
    }

    private void clearCache() {
        bucketExists = null;
        stats = null;
        lastModified = null;
        size = null;
    }

    private static class ItemIterator implements Iterator<Resource> {

        private final S3Resource parent;
        private final Iterator<Result<Item>> items;

        public ItemIterator(S3Resource parent, Iterator<Result<Item>> items) {
            requireNonNull(parent);
            requireNonNull(items);
            this.parent = parent;
            this.items = items;
        }

        @Override
        public boolean hasNext() {
            return items.hasNext();
        }

        private String cleanupEtag(String value) {
            if (value == null) return null;
            if (value.startsWith("\"")) value = value.substring(1);
            if (value.endsWith("\"")) value = value.substring(0, value.length() - 1);
            return value;
        }

        private String cleanupOwner(Owner owner) {
            if (owner == null) return null;
            return owner.id();
        }

        @Override
        public Resource next() {
            try {
                Item item = items.next().get();
                String etag = cleanupEtag(item.etag());
                String owner = cleanupOwner(item.owner());
                Resource object;
                ZonedDateTime lastModified = ZonedDateTime.now();
                try {
                    lastModified = item.lastModified();
                } catch (NullPointerException e) {
                    // why is this field empty sometime?
                }
                String name = FileUtils.getFileName(removeStartSlash(item.objectName()));
                if (item.isDir()) {
                    object = parent.resolve(name, Type.DIRECTORY);
                    ((S3Resource) object).update(lastModified, null, etag, owner);
                } else {
                    object = parent.resolve(name, Type.FILE);
                    ((S3Resource) object).update(lastModified, item.size(), etag, owner);
                }
                return object;
            } catch (Exception e) {
                return ExceptionUtils.throwException(e);
            }
        }
    }

    private class S3OutputStream extends OutputStream {

        private final File file;
        private final OutputStream delegate;

        public S3OutputStream(File file, OutputStream delegate) {
            this.file = file;
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
        }

        @Override
        public void write(@NotNull byte[] b) throws IOException {
            delegate.write(b);
        }

        @Override
        public void write(@NotNull byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
            try {
                uploadFile(file);
            } catch (Exception e) {
                FileUtils.remove(file);
            }
        }
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

    private static final String NO_SUCH_BUCKET = "NoSuchBucket";
    private static final String NO_SUCH_KEY = "NoSuchKey";
}
