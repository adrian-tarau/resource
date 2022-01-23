package net.tarau.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static net.tarau.resource.ResourceUtils.getInputStreamAsBytes;
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
    void exists() {
        Resource resource = ClassPathResource.file("dir1/file11.txt");
        assertTrue(resource.exists());
        resource = ClassPathResource.file("dir1/file111.txt");
        assertFalse(resource.exists());
    }

    @Test
    void list() {
        Resource resource = ClassPathResource.file("dir1/file11.txt");
        assertEquals(0, resource.list().size());
        resource = ClassPathResource.directory("dir3");
        assertEquals(2, resource.list().size());
    }

    @Test
    void resolve() {
        Resource resource = ClassPathResource.file("dir1/");
        resource = resource.resolve("file11.txt");
        assertEquals("file11.txt", resource.getFileName());
    }

    @Test
    void lastModified() {
        Resource resource = ClassPathResource.file("dir1/file11.txt");
        assertTrue(resource.lastModified() > 1642899716720L);
    }

    @Test
    void length() {
        Resource resource = ClassPathResource.file("dir1/file11.txt");
        assertEquals(4, resource.length());
    }

    @Test
    void toURI() {
        Resource resource = ClassPathResource.file("dir1/file11.txt");
        assertTrue(resource.toURI().toASCIIString().endsWith("file11.txt"));
    }
}