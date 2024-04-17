package net.microfalx.resource.rocksdb;

import net.microfalx.lang.TimeUtils;
import net.microfalx.resource.ResourceException;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.microfalx.lang.ArgumentUtils.requireBounded;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.FileUtils.validateDirectoryExists;
import static net.microfalx.lang.FormatterUtils.formatBytes;
import static org.rocksdb.CompressionType.SNAPPY_COMPRESSION;

/**
 * A singleton which manages a collection of RocksDB instances.
 */
public class RocksDbManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDbManager.class);

    private static final long CLEANUP_INTERVAL = 60_000;

    private static final RocksDbManager INSTANCE = new RocksDbManager();

    private volatile boolean initialized;
    private final Map<File, WeakReference<RocksDB>> databases = new ConcurrentHashMap<>();
    private final ReferenceQueue<RocksDB> cleanupQueue = new ReferenceQueue<>();
    private volatile long lastCleanup = System.currentTimeMillis();
    private volatile long maximumSize = 50_000_000;

    /**
     * Returns an instance to the manager.
     *
     * @return a non-null instance
     */
    public static RocksDbManager getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the maximum size of a value.
     *
     * @return a positive integer
     */
    public long getMaximumSize() {
        return maximumSize;
    }

    /**
     * Changes the meximum size for a value
     *
     * @param maximumSize a positive integer
     */
    public void setMaximumSize(long maximumSize) {
        requireBounded(maximumSize, 0, Runtime.getRuntime().maxMemory());
        this.maximumSize = maximumSize;
        LOGGER.info("Maximum size for a value is " + formatBytes(maximumSize));
    }

    /**
     * Returns opened databases.
     *
     * @return a non-null instance
     */
    public Collection<RocksDB> list() {
        Collection<RocksDB> dbs = new ArrayList<>();
        for (WeakReference<RocksDB> reference : databases.values()) {
            RocksDB db = reference.get();
            if (db != null) dbs.add(db);
        }
        return dbs;
    }

    /**
     * Returns the number of tuples (estimation) in the database in memory (MemTable) and on disk (SST).
     *
     * @param db the database
     * @return the size in bytes
     */
    public static long getDiskCount(RocksDB db) {
        long count;
        try {
            count = db.getLongProperty("rocksdb.estimate-num-keys") - db.getLongProperty("rocksdb.num-entries-imm-mem-tables");
        } catch (RocksDBException e) {
            count = -1;
        }
        return count;
    }

    /**
     * Returns the size of the database on disk (SST).
     *
     * @param db the database
     * @return the size in bytes
     */
    public static long getDiskSize(RocksDB db) {
        requireNonNull(db);
        long count;
        try {
            count = db.getLongProperty("rocksdb.total-sst-files-size");
        } catch (RocksDBException e) {
            count = -1;
        }
        return count;
    }

    /**
     * Returns the number of tuples (estimation) of memory tables in the database.
     *
     * @param db the database
     * @return the size in bytes
     */
    public static long getMemoryCount(RocksDB db) {
        requireNonNull(db);
        long count;
        try {
            count = db.getLongProperty("rocksdb.num-entries-active-mem-table") + db.getLongProperty("rocksdb.num-entries-imm-mem-tables");
        } catch (RocksDBException e) {
            count = -1;
        }
        return count;
    }


    /**
     * Returns the size (estimation) of memory tables in the database.
     *
     * @param db the database
     * @return the size in bytes
     */
    public static long getMemorySize(RocksDB db) {
        requireNonNull(db);
        long count;
        try {
            count = db.getLongProperty("rocksdb.size-all-mem-tables");
        } catch (RocksDBException e) {
            count = -1;
        }
        return count;
    }

    /**
     * Returns a multi-line string containing a description of the internal stats
     *
     * @param db the database
     * @return the stats
     */
    public static String getStats(RocksDB db) {
        requireNonNull(db);
        String stats;
        try {
            stats = db.getProperty("rocksdb.stats");
        } catch (RocksDBException e) {
            stats = "#ERROR";
        }
        return stats;

    }

    /**
     * Creates a RocksDB database.
     *
     * @param file the directory which will contain the database
     * @return a non-null instance
     */
    public RocksDB create(File file) {
        requireNonNull(file);
        initialize();
        validateDirectoryExists(file);
        final Options options = new Options();
        options.setCompressionType(SNAPPY_COMPRESSION);
        options.setBlobCompressionType(SNAPPY_COMPRESSION);
        options.setBottommostCompressionType(SNAPPY_COMPRESSION);
        options.setCreateIfMissing(true);
        try {
            RocksDB db = RocksDB.open(options, file.getAbsolutePath());
            LOGGER.info("Open RocksDB database at '" + file.getAbsolutePath() + ", compression=" + db.getOptions().compressionType()
                    + ", blob compression=" + db.getOptions().blobCompressionType());
            return db;
        } catch (RocksDBException e) {
            throw new ResourceException("Failed to initialize RocksDB database at '" + file + "'", e);
        }
    }

    /**
     * Returns the database associated with the file.
     *
     * @param file the file where to store the database
     * @return the database
     */
    public RocksDB get(File file) {
        requireNonNull(file);
        synchronized (databases) {
            cleanup(false);
            RocksDB db = null;
            WeakReference<RocksDB> reference = databases.get(file);
            if (reference != null) db = reference.get();
            if (db != null) return db;
            db = create(file);
            reference = new WeakReference<>(db, cleanupQueue);
            databases.put(file, reference);
            return db;
        }
    }

    /**
     * Releases unused databases.
     *
     * @param force {@code true} to force a cleanup, {@code false} otherwise
     */
    public void cleanup(boolean force) {
        if (TimeUtils.millisSince(lastCleanup) < CLEANUP_INTERVAL && !force) return;
        for (; ; ) {
            Reference<? extends RocksDB> reference = cleanupQueue.poll();
            if (reference == null) break;
            RocksDB db = reference.get();
            try {
                if (db != null) db.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close RocksDB '" + db + "'", e);
            }
        }
        lastCleanup = System.currentTimeMillis();
    }

    private void initialize() {
        if (!initialized) {
            RocksDB.loadLibrary();
            setMaximumSize(Runtime.getRuntime().maxMemory() / 5);
        }
        initialized = true;
    }


}
