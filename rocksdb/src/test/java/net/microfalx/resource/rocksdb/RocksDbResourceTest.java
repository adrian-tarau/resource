package net.microfalx.resource.rocksdb;

import net.microfalx.resource.FileResource;
import net.microfalx.resource.Resource;
import net.microfalx.resource.ResourceFactory;
import net.microfalx.resource.SharedResource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RocksDbResourceTest extends AbstractRocksDbTest {

    @Test
    void createFactory() {
        Resource resource = RocksDbResource.file(createDbResource(), "test");
        assertNotNull(resource);
        resource = RocksDbResource.file(FileResource.directory(createDbDirectory()), "test");
        assertNotNull(resource);
    }

    @Test
    void shared() throws IOException {
        Resource shared = SharedResource.directory("rocksdb");
        Resource resource = RocksDbResource.create(shared);
        Resource test = resource.resolve("test");
        test.create();
        assertTrue(test.exists());
    }

    @Test
    void symlink() throws IOException {
        Resource shared = SharedResource.directory("rocksdb");
        ResourceFactory.registerSymlink("rocksdb", RocksDbResource.create(shared));
        ResourceFactory.registerSymlink("rocksdb", RocksDbResource.create(shared));
        Resource test = shared.resolve("test");
        test.create();
        assertTrue(test.exists());

        shared.resolve("test").delete();
        assertFalse(test.exists());

        test = shared.resolve("test");
        test.create();
        assertTrue(test.exists());
    }

    @Test
    void resolve() {
        Resource resource = RocksDbResource.file(createDbResource(), "test");
        Resource resolvedResource = ResourceFactory.resolve(resource.toURI());
        assertEquals(resource, resolvedResource);
        Resource childResource = resource.resolve("child1");
        assertEquals("test/child1", childResource.getPath());
        childResource = childResource.resolve("child1", Resource.Type.FILE);
        assertEquals("test/child1/child1", childResource.getPath());
        assertEquals("child1", childResource.getFileName());
    }

    @Test
    void write() throws IOException {
        Resource resource = RocksDbResource.file(createDbResource(), "test");
        assertFalse(resource.exists());
        assertEquals(0, resource.getInputStream().readAllBytes().length);
        writeData(resource);
        assertTrue(resource.exists());
        assertArrayEquals(VALUE, resource.getInputStream().readAllBytes());
    }

    @Test
    void delete() throws IOException {
        Resource resource = RocksDbResource.file(createDbResource(), "test");
        assertFalse(resource.exists());
        assertEquals(0, resource.getInputStream().readAllBytes().length);
        writeData(resource);
        assertTrue(resource.exists());
        resource.delete();
        assertFalse(resource.exists());
    }

    @Test
    void length() throws IOException {
        Resource resource = RocksDbResource.file(createDbResource(), "test");
        assertFalse(resource.exists());
        assertEquals(0, resource.length());
        writeData(resource);
        assertEquals(1000, resource.length());
    }

    @Test
    void lastModified() throws IOException {
        Resource resource = RocksDbResource.file(createDbResource(), "test");
        assertEquals(0, resource.lastModified());
        writeData(resource);
        assertTrue(resource.lastModified() > 1707681611235L);
    }

    @Test
    void walk() throws IOException {
        Resource resource = RocksDbResource.file(createDbResource(), "test");
        AtomicInteger count = new AtomicInteger();
        resource.walk((root, child) -> {
            count.incrementAndGet();
            return true;
        });
        assertEquals(0, count.get());
    }

    @Test
    void list() throws IOException {
        Resource resource = RocksDbResource.file(createDbResource(), "test");
        AtomicInteger count = new AtomicInteger();
        assertEquals(0, resource.list().size());
    }

    @Test
    void get() throws IOException {
        Resource resource = RocksDbResource.file(createDbResource(), "test");
        Resource absoluteResource = resource.get("test2");
        assertEquals("test2", absoluteResource.getPath());
        absoluteResource = resource.get("test3", Resource.Type.FILE);
        assertEquals("test3", absoluteResource.getPath());
    }

    @Test
    void create() throws IOException {
        Resource resource = RocksDbResource.file(createDbResource(), "test");
        assertFalse(resource.exists());
        resource.create();
        assertTrue(resource.exists());
        resource.createParents();
        assertTrue(resource.exists());
    }

    private void writeData(Resource resource) throws IOException {
        OutputStream outputStream = resource.getOutputStream();
        outputStream.write(VALUE);
        outputStream.close();
    }

}