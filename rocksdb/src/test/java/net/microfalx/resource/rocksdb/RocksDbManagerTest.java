package net.microfalx.resource.rocksdb;

import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RocksDbManagerTest extends AbstractRocksDbTest {

    private final byte[] KEY1 = "key1".getBytes();
    private final byte[] KEY2 = "key2".getBytes();
    private final byte[] KEY3 = "key3".getBytes();

    @Test
    void open() throws IOException {
        File file = createDbDirectory();
        RocksDB db = RocksDbManager.getInstance().get(file);
        assertNotNull(db);
    }

    @Test
    void access() throws RocksDBException {
        File file = createDbDirectory();
        RocksDB db = RocksDbManager.getInstance().get(file);
        db.put(KEY1, VALUE);
        db.put(KEY2, VALUE);
        assertNull(db.get(KEY3));
        assertArrayEquals(VALUE, db.get(KEY1));
        assertArrayEquals(VALUE, db.get(KEY2));
    }

    @Test
    void dump() throws RocksDBException {
        File file = createDbDirectory();
        RocksDB db = RocksDbManager.getInstance().get(file);
        for (int i = 0; i < 5000; i++) {
            db.put(Integer.toString(i).getBytes(), VALUE);
        }
        for (int i = 0; i < 5000; i++) {
            assertNotNull(db.get(Integer.toString(i).getBytes()));
        }
    }

    @Test
    void cleanup() throws RocksDBException, InterruptedException {
        File file = createDbDirectory();
        RocksDbManager.getInstance().get(file);
        assertTrue(RocksDbManager.getInstance().list().size() > 0);
        for (int i = 0; i < 10; i++) {
            System.gc();
            Thread.sleep(100);
        }
        RocksDbManager.getInstance().cleanup(true);
        assertEquals(0, RocksDbManager.getInstance().list().size());
    }


}