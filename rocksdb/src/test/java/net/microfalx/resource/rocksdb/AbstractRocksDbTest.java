package net.microfalx.resource.rocksdb;

import net.microfalx.resource.FileResource;
import net.microfalx.resource.Resource;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractRocksDbTest {

    protected final byte[] VALUE = new byte[1000];

    @BeforeEach
    void before() {
        ThreadLocalRandom.current().nextBytes(VALUE);
    }

    protected final Resource createDbResource() {
        return FileResource.directory(createDbDirectory());
    }

    protected final File createDbDirectory() {
        File file = new File(System.getProperty("java.io.tmpdir"));
        File directory = new File(file, "rocksdb");
        return new File(directory, Long.toString(System.currentTimeMillis(), Character.MAX_RADIX));
    }
}
