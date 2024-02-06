package net.microfalx.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static net.microfalx.lang.IOUtils.getInputStreamAsBytes;
import static org.junit.jupiter.api.Assertions.*;

class UrlResourceTest {

    @Test
    void create() throws IOException {
        Resource resource = UrlResource.create(ClassPathResource.file("file11.txt").toURI().toURL());
        assertNotNull(resource);
        resource = UrlResource.create(ClassPathResource.file("file11.txt").toURI());
        assertNotNull(resource);
        resource = UrlResource.create(ClassPathResource.directory("dir1").toURI().toURL(), Resource.Type.DIRECTORY);
        assertNotNull(resource);
    }

    @Test
    void getParent() {
        Resource resource = ClassPathResource.file("dir1/file11.txt");
        assertTrue(resource.getParent().getPath().endsWith("dir1"));
    }

    @Test
    void getInputStream() throws IOException {
        Resource resource = ClassPathResource.file("dir1/file11.txt");
        assertEquals(4, getInputStreamAsBytes(resource.getInputStream()).length);
    }

    @Test
    void getFileName() {
        Resource resource = ClassPathResource.file("dir1/file11.txt");
        assertEquals("file11.txt", resource.getName());
        assertEquals("file11.txt", resource.getFileName());
        resource = ClassPathResource.file("dir1/");
        assertEquals("dir1", resource.getFileName());
    }

    @Test
    void exists() throws IOException {
        Resource resource = ClassPathResource.file("dir1/file11.txt");
        assertTrue(resource.exists());
        resource = ClassPathResource.file("dir1/file111.txt");
        assertFalse(resource.exists());
    }

    @Test
    void list() throws IOException {
        Resource resource = ClassPathResource.file("dir1/file11.txt");
        assertEquals(0, resource.list().size());
        resource = ClassPathResource.directory("dir3");
        assertEquals(3, resource.list().size());
    }

    @Test
    void resolve() {
        Resource resource = ClassPathResource.file("dir1/");
        resource = resource.resolve("file11.txt");
        assertEquals("file11.txt", resource.getFileName());
    }

    @Test
    void lastModified() throws IOException {
        Resource resource = ClassPathResource.file("dir1/file11.txt");
        assertTrue(resource.lastModified() > 1642899716720L);
    }

    @Test
    void length() throws IOException {
        Resource resource = ClassPathResource.file("dir1/file11.txt");
        assertEquals(4, resource.length());
    }

    @Test
    void toURI() {
        Resource resource = ClassPathResource.file("dir1/file11.txt");
        assertTrue(resource.toURI().toASCIIString().endsWith("file11.txt"));
    }

    @Test
    void walkDirectory() throws IOException {
        Resource directory = ClassPathResource.directory("dir3");
        AtomicInteger directoryCount = new AtomicInteger();
        AtomicInteger fileCount = new AtomicInteger();
        directory.walk((root, child) -> {
            if (child.isFile()) {
                fileCount.incrementAndGet();
            } else {
                directoryCount.incrementAndGet();
            }
            return true;
        });
        assertEquals(3, directoryCount.get());
        assertEquals(4, fileCount.get());
    }

    @Test
    void walkDirectoryInsideJars() throws IOException {
        Resource directory = ClassPathResource.directory("META-INF/maven");
        AtomicInteger directoryCount = new AtomicInteger();
        AtomicInteger fileCount = new AtomicInteger();
        directory.walk((root, child) -> {
            if (child.isFile()) {
                fileCount.incrementAndGet();
            } else {
                directoryCount.incrementAndGet();
            }
            return true;
        });
        assertEquals(40, directoryCount.get());
        assertEquals(40, fileCount.get());
    }
}