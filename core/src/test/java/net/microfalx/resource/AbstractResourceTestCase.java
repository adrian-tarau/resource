package net.microfalx.resource;

import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractResourceTestCase {

    private final AtomicInteger fileCount = new AtomicInteger();
    private final AtomicInteger directoryCount = new AtomicInteger();
    protected final ResourceVisitor visitor = new ResourceVisitorImpl();

    @BeforeEach
    void before() {
        fileCount.set(0);
        directoryCount.set(0);
    }

    final void assertCount(int fileCount, int directoryCount) {
        assertEquals(fileCount, this.fileCount.get());
        assertEquals(directoryCount, this.directoryCount.get());
        this.fileCount.set(0);
        this.directoryCount.set(0);
    }

    final Resource fromFile(String path) {
        return ClassPathResource.file(path).toFile();
    }

    final Resource fromDirectory(String path) {
        return ClassPathResource.directory(path).toFile();
    }

    class ResourceVisitorImpl implements ResourceVisitor {

        @Override
        public boolean onResource(Resource root, Resource child) throws IOException {
            assertTrue(child.isFile() ? child.length() > 0 : true, child.getName());
            assertTrue(child.lastModified() > 0, child.getName());
            if (child.isFile()) {
                fileCount.incrementAndGet();
            } else {
                directoryCount.incrementAndGet();
            }
            return true;
        }
    }
}
