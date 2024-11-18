package net.microfalx.resource.rocksdb;

import net.microfalx.lang.FileUtils;
import net.microfalx.lang.StringUtils;
import net.microfalx.metrics.Metrics;
import net.microfalx.resource.*;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;

import static java.lang.System.currentTimeMillis;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.StringUtils.*;
import static net.microfalx.resource.ResourceUtils.SLASH;

/**
 * A resource implemented on top of RockDB key/value data store.
 * <p>
 * The resource only supports files since each file is actually stored in the database under a key which points to the
 * resource content. However,
 * <p>
 * This type of resource should be used to store tens of millions of small files in the local file system without the cost
 * of managing so many entries at the OS level.
 */
public class RocksDbResource extends AbstractResource {

    private static final int HEADER_SIZE = 16;
    private static final byte[] EMPTY_BUFFER = new byte[0];

    private static final Metrics METRICS = ResourceUtils.METRICS.withGroup("ClassPath");

    private final Resource resource;
    private final String path;

    private RocksDB db;

    /**
     * Creates a root resource.
     *
     * @param resource the directory where the database is located
     * @return a non-null instance
     */
    public static Resource create(Resource resource) {
        return create(resource, SLASH, Type.DIRECTORY);
    }

    /**
     * Creates a file resource at a given path associated with a database.
     *
     * @param resource the directory where the database is located
     * @param path     the path of the resource.
     * @return a non-null instance
     */
    public static Resource file(Resource resource, String path) {
        return create(resource, path, Type.FILE);
    }

    /**
     * Creates a directory resource at a given path associated with a database.
     *
     * @param resource the directory where the database is located
     * @param path     the path of the resource.
     * @return a non-null instance
     */
    public static Resource directory(Resource resource, String path) {
        return create(resource, path, Type.DIRECTORY);
    }

    /**
     * Creates a resource at a given path associated with a database.
     *
     * @param resource the directory where the database is located
     * @param path     the path of the resource.
     * @return a non-null instance
     */
    public static Resource create(Resource resource, String path, Type type) {
        requireNonNull(resource);
        requireNonNull(type);
        path = removeStartSlash(defaultIfEmpty(path, EMPTY_STRING));
        if (!resource.isLocal()) {
            throw new ResourceException("The database location should point to a local file system");
        }
        if (resource instanceof SharedResource) resource = ((SharedResource) resource).getDelegatingResource(false);
        resource = resource.toFile();

        return new RocksDbResource(Type.FILE, StringUtils.toIdentifier(path), resource, path);
    }

    RocksDbResource(Type type, String id, Resource resource, String path) {
        super(type, id);
        this.resource = resource;
        this.path = defaultIfNull(path, SLASH);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getFileName() {
        return FileUtils.getFileName(path);
    }

    @Override
    protected boolean doExists() throws IOException {
        if (SLASH.equals(getId())) {
            return true;
        } else {
            return getDb().keyExists(getId().getBytes());
        }
    }

    @Override
    protected InputStream doGetInputStream(boolean raw) throws IOException {
        try (ReadOptions options = new ReadOptions()) {
            RocksDB db = getDb();
            byte[] content = db.get(options, getId().getBytes());
            if (content != null) {
                return new ByteArrayInputStream(content, HEADER_SIZE, content.length - HEADER_SIZE);
            } else {
                return new ByteArrayInputStream(EMPTY_BUFFER);
            }
        } catch (RocksDBException e) {
            throw new IOException("Failed to read content of resource '" + getPath() + "' from database '" + resource.getPath() + "'", e);
        }
    }

    @Override
    protected OutputStream doGetOutputStream() throws IOException {
        return new OutputStreamImpl(new ByteArrayOutputStream());
    }

    @Override
    protected void doDelete() throws IOException {
        if (isRoot()) return;
        try {
            getDb().delete(getId().getBytes());
        } catch (RocksDBException e) {
            throw new IOException("Failed to remove resource '" + getPath() + "' from database '" + resource.getPath() + "'", e);
        }
    }

    @Override
    protected void doCreate() throws IOException {
        if (isRoot()) {
            try {
                getDb().resetStats();
            } catch (RocksDBException e) {
                throw new IOException("Failed to initialize root resource for " + this, e);
            }
        } else {
            doGetOutputStream().close();
        }
    }

    @Override
    protected void doCreateParents() throws IOException {
        // nothing to do
    }

    @Override
    protected long doLastModified() throws IOException {
        try {
            byte[] buffer = new byte[HEADER_SIZE];
            getDb().get(getId().getBytes(), buffer);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer));
            dis.skipBytes(4);
            return dis.readLong();
        } catch (RocksDBException e) {
            throw new IOException("Failed to get resource last modified '" + getPath() + "' from database '" + resource.getPath() + "'", e);
        }
    }

    @Override
    protected long doLength() throws IOException {
        try {
            byte[] buffer = new byte[HEADER_SIZE];
            int size = getDb().get(getId().getBytes(), buffer);
            if (size == RocksDB.NOT_FOUND) return 0;
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer));
            return dis.readInt();
        } catch (RocksDBException e) {
            throw new IOException("Failed to get resource length '" + getPath() + "' from database '" + resource.getPath() + "'", e);
        }
    }

    @Override
    protected Collection<Resource> doList() throws IOException {
        return Collections.emptyList();
    }

    @Override
    public Resource get(String path, Type type) {
        requireNonNull(path);
        return RocksDbResource.create(resource, path, type);
    }

    @Override
    public URI toURI() {
        URI uri = resource.toURI();
        try {
            return new URI(RocksDbUtilities.ROCKSDB, null, uri.getPath(), path);
        } catch (URISyntaxException e) {
            throw new ResourceException("Failed to create resource URI for database " + resource + "#" + path, e);
        }
    }

    @Override
    protected Metrics getMetrics() {
        return METRICS;
    }

    private RocksDB getDb() {
        if (db == null) {
            File directory = ((FileResource) resource).getFile();
            db = RocksDbManager.getInstance().get(directory);
        }
        return db;
    }

    private boolean isRoot() {
        return ResourceUtils.isRoot(getPath());
    }

    private void writeContent(byte[] data) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream doo = new DataOutputStream(buffer);
        doo.writeInt(data.length - HEADER_SIZE);
        doo.writeLong(currentTimeMillis());
        byte[] header = buffer.toByteArray();
        System.arraycopy(header, 0, data, 0, header.length);
        try (WriteOptions options = new WriteOptions()) {
            RocksDB db = getDb();
            db.put(options, getId().getBytes(), data);
        } catch (RocksDBException e) {
            throw new IOException("Failed to write content for database " + resource + "#" + path, e);
        }
    }

    class OutputStreamImpl extends BufferedOutputStream {

        public OutputStreamImpl(OutputStream out) throws IOException {
            super(out);
            DataOutputStream doo = new DataOutputStream(out);
            byte[] buffer = new byte[HEADER_SIZE];
            doo.write(buffer);
        }

        @Override
        public void close() throws IOException {
            super.close();
            ByteArrayOutputStream outputStream = (ByteArrayOutputStream) out;
            writeContent(outputStream.toByteArray());
        }
    }

    public static class RocksDbResourceResolver implements ResourceResolver {

        @Override
        public boolean supports(URI uri) {
            return RocksDbUtilities.ROCKSDB.equalsIgnoreCase(uri.getScheme());
        }

        @Override
        public Resource resolve(URI uri, Resource.Type type) {
            File directory = new File(ResourceUtils.toFileUri(uri));
            return RocksDbResource.create(FileResource.directory(directory), uri.getRawFragment(), type)
                    .withFragment(uri.getFragment());
        }
    }
}
