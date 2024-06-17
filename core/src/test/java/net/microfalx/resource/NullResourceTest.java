package net.microfalx.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class NullResourceTest {

    @Test
    void createNull() {
        assertNotNull(createNullResource());
    }

    @Test
    void getFileName() {
        Resource resource = createNullResource();
        assertNotNull(resource.getFileName());
    }

    @Test
    void doGetInputStream() throws IOException {
        Resource resource = createNullResource();
        assertNotNull(resource.getInputStream());
        assertNotNull(resource.getInputStream(true));
    }

    @Test
    void doGetOutputStream() throws IOException {
        Resource resource = createNullResource();
        assertNotNull(resource.getOutputStream());
    }

    @Test
    void doExists() throws IOException {
        Resource resource = createNullResource();
        assertFalse(resource.exists());
    }

    @Test
    void doLastModified() throws IOException {
        Resource resource = createNullResource();
        assertEquals(0L, resource.lastModified());
    }

    @Test
    void doLength() throws IOException {
        Resource resource = createNullResource();
        assertEquals(0L, resource.length());
    }

    @Test
    void doList() throws IOException {
        Resource resource = createNullResource();
        assertIterableEquals(Collections.EMPTY_LIST, resource.list());
    }

    @Test
    void resolve() {
        Resource resource = createNullResource();
        assertEquals(Resource.NULL, resource.resolve(resource.getPath()));
        assertEquals(Resource.NULL, resource.resolve(resource.getPath(), Resource.Type.FILE));
    }

    @Test
    void get() {
        Resource resource = createNullResource();
        assertEquals(Resource.NULL, resource.get(resource.getPath()));
        assertEquals(Resource.NULL, resource.get(resource.getPath(), Resource.Type.FILE));
    }

    @Test
    void toURI() {
        URI uri = createNullResource().toURI();
        assertEquals("file",uri.getScheme());
        assertEquals("/dev/null",uri.getPath());
        assertEquals(-1,uri.getPort());
        assertNull(uri.getAuthority());
        assertNull(uri.getHost());
    }

    private Resource createNullResource() {
        return NullResource.createNull();
    }
}