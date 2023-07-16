package net.microfalx.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FileResourceTest {

    private AtomicInteger fileCount = new AtomicInteger();
    private AtomicInteger directoryCount = new AtomicInteger();
    private ResourceVisitor visitor = new FileVisitor();

    @Test
    void file() throws IOException {
        Resource file = fromFile("file1.txt");
        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertFalse(file.isDirectory());
        assertEquals(4, file.length());
        assertNotEquals(0, file.lastModified());
        assertEquals("file1.txt", file.getFileName());

        file = fromFile("dir1/file11.txt");
        assertTrue(file.exists());
        assertEquals(4, file.length());
        assertEquals("file11.txt", file.getFileName());
        assertEquals("dir1", file.getParent().getFileName());
    }

    @Test
    void directory() throws IOException {
        Resource directory = ClassPathResource.directory("dir1");
        assertTrue(directory.exists());
        assertFalse(directory.isFile());
        assertTrue(directory.isDirectory());
        assertEquals(0, directory.length());
        assertNotEquals(0, directory.lastModified());
    }

    @Test
    void walk() throws IOException {
        Resource dir1 = fromFile("dir1");
        dir1.walk(visitor);
        assertCount(1, 0);
        dir1 = fromFile("dir3");
        dir1.walk(visitor, 1);
        assertCount(1, 2);
        dir1.walk(visitor, 2);
        assertCount(3, 3);
        dir1.walk(visitor, 3);
        assertCount(4, 3);
        dir1.walk(visitor);
        assertCount(4, 3);
    }

    @Test
    void list() throws IOException {
        Resource dir1 = fromFile("dir1");
        assertEquals(1, dir1.list().size());
        dir1 = fromFile("dir3");
        assertEquals(3, dir1.list().size());
    }

    private void assertCount(int fileCount, int directoryCount) {
        assertEquals(fileCount, this.fileCount.get());
        assertEquals(directoryCount, this.directoryCount.get());
        this.fileCount.set(0);
        this.directoryCount.set(0);
    }

    private Resource fromFile(String path) {
        return ClassPathResource.file(path).toFile();
    }

    private Resource fromDirectory(String path) {
        return ClassPathResource.directory(path).toFile();
    }

    class FileVisitor implements ResourceVisitor {
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