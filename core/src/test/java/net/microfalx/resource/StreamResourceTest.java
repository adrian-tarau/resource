package net.microfalx.resource;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class StreamResourceTest {

    @Test
    void createWithInputStream() {
        Resource resource = StreamResource.create(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}));
        assertNotNull(resource);
    }

    @Test
    void createWithInputStreamAndFileName() throws IOException {
        Resource fileResource = FileResource.file(new File("src" + File.separator +
                "test" + File.separator + "resources" + File.separator + "file1.txt"));
        Resource streamResource = StreamResource.create(new ByteArrayInputStream(fileResource.loadAsBytes()),
                fileResource.getFileName());
        assertEquals("file1.txt", streamResource.getFileName());
        assertEquals("text", streamResource.loadAsString());
        assertEquals("text/plain", streamResource.getMimeType());
        assertEquals("text/plain", streamResource.detectMimeType());
    }

    @Test
    void createFromCallable() {
        Resource resource = StreamResource.create(() -> new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}));
        assertThrows(IOException.class, () -> resource.getInputStream().readAllBytes());
    }

    @Test
    void createFromCallableAndFileName() throws IOException {
        Resource fileResource = FileResource.file(new File("src" + File.separator +
                "test" + File.separator + "resources" + File.separator + "file1.txt"));
        Resource streamResource = StreamResource.create(() -> new ByteArrayInputStream(fileResource.loadAsBytes()),
                fileResource.getFileName());
        assertEquals("file1.txt", streamResource.getFileName());
        assertEquals("text", streamResource.loadAsString());
        assertEquals("text/plain", streamResource.getMimeType());
        assertEquals("text/plain", streamResource.detectMimeType());
    }


    @Test
    void getFileName() {
        Resource resource = StreamResource.create(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}));
        assertNotNull(resource.getFileName());
    }

    @Test
    void doGetInputStream() throws IOException {
        Resource resource = StreamResource.create(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}));
        assertNotNull(resource.getInputStream());
    }

    @Test
    void doExists() throws IOException {
        Resource resource = StreamResource.create(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}));
        assertTrue(resource.exists());
    }

    @Test
    void doLastModified() throws IOException {
        Resource resource = StreamResource.create(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}));
        assertNotNull(resource.lastModified());
    }

    @Test
    void doLength() throws IOException {
        Resource resource = StreamResource.create(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}));
        assertNotNull(resource.length());
    }

    @Test
    void doList() throws IOException {
        Resource resource = StreamResource.create(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}));
        assertIterableEquals(Collections.EMPTY_LIST, resource.list());
    }

    @Test
    void resolve() {
        Resource resource = StreamResource.create(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}));
        assertEquals(Resource.NULL, resource.resolve("", null));
    }

    @Test
    void get() {
        Resource resource = StreamResource.create(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}));
        assertEquals(Resource.NULL, resource.get("", null));
    }

    @Test
    void toURI() {
        Resource resource = StreamResource.create(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}));
        assertEquals("memory", resource.toURI().getScheme());
        assertNotNull(resource.toURI().getHost());
        assertEquals(-1, resource.toURI().getPort());
        assertNotNull(resource.toURI().getPath());
    }

}