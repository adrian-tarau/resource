package net.microfalx.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class MemoryResourceTest {


    public static final long LAST_MODIFIED_TIME = LocalDateTime.of(2020, 8, 12, 17, 8,
                    34).toEpochSecond(ZoneOffset.UTC);

    @Test
    void createFromText() throws IOException {
        Resource resource = addMemoryResourceProperties(MemoryResource.create("I am writing java code"));
        assertEquals("Memory Resource", resource.getName());
        assertEquals("text/plain", resource.getMimeType());
        assertEquals("text/plain", resource.detectMimeType());
        assertEquals(22L, resource.length());
        assertTrue(resource.exists());
        assertEquals(Resource.Type.FILE, resource.getType());
        assertTrue(resource.isReadable());
        assertTrue(resource.isWritable());
        assertFalse(resource.isLocal());
        assertEquals("memory", resource.toURI().getScheme());
        assertEquals(-1, resource.toURI().getPort());
        assertEquals("This is a memory resource", resource.getDescription());
        assertNull(resource.getFileExtension());
        assertNotNull(resource.getWriter());
        assertNotNull(resource.getInputStream());
        assertIterableEquals(Collections.EMPTY_LIST, resource.list());
        assertArrayEquals(new byte[]{73, 32, 97, 109, 32, 119, 114, 105, 116, 105, 110, 103, 32, 106, 97, 118, 97, 32,
                99, 111, 100, 101}, resource.loadAsBytes());
        UserPasswordCredential credential = (UserPasswordCredential) resource.getCredential();
        assertEquals("username", credential.getUserName());
        assertEquals("password", credential.getPassword());
        assertEquals(Resource.NULL,resource.resolve(resource.getPath(), Resource.Type.FILE));
        assertEquals(Resource.NULL,resource.resolve(resource.getPath()));
        assertEquals(Resource.NULL,resource.get(resource.getPath()));
        assertEquals(Resource.NULL,resource.get(resource.getPath(), Resource.Type.FILE));
        assertEquals("f257484f158e1fb73e256d0fd84b1b12",resource.toHash());
    }

    @Test
    void createFromTextWithFileName() throws IOException {
        Resource resource = addMemoryResourceProperties(MemoryResource.create("I am writing java code",
                "text.txt"));
        assertEquals("Memory Resource", resource.getName());
        assertEquals("text/plain", resource.getMimeType());
        assertEquals("text/plain", resource.detectMimeType());
        assertEquals(22L, resource.length());
        assertTrue(resource.exists());
        assertEquals(Resource.Type.FILE, resource.getType());
        assertTrue(resource.isReadable());
        assertTrue(resource.isWritable());
        assertFalse(resource.isLocal());
        assertEquals("memory", resource.toURI().getScheme());
        assertEquals(-1, resource.toURI().getPort());
        assertEquals("This is a memory resource", resource.getDescription());
        assertEquals("txt", resource.getFileExtension());
        assertNotNull(resource.getWriter());
        assertNotNull(resource.getInputStream());
        assertEquals("text.txt", resource.getFileName());
        assertIterableEquals(Collections.EMPTY_LIST, resource.list());
        assertArrayEquals(new byte[]{73, 32, 97, 109, 32, 119, 114, 105, 116, 105, 110, 103, 32, 106, 97, 118, 97, 32,
                99, 111, 100, 101}, resource.loadAsBytes());
        UserPasswordCredential credential = (UserPasswordCredential) resource.getCredential();
        assertEquals("username", credential.getUserName());
        assertEquals("password", credential.getPassword());
        assertEquals(Resource.NULL,resource.resolve(resource.getPath(), Resource.Type.FILE));
        assertEquals(Resource.NULL,resource.resolve(resource.getPath()));
        assertEquals(Resource.NULL,resource.get(resource.getPath()));
        assertEquals(Resource.NULL,resource.get(resource.getPath(), Resource.Type.FILE));
        assertEquals("f257484f158e1fb73e256d0fd84b1b12",resource.toHash());
    }

    @Test
    void createFromTextWithFileNameAndLastModified() throws IOException {
        Resource resource = addMemoryResourceProperties(MemoryResource.create("I am writing java code",
                "text.txt", LAST_MODIFIED_TIME));
        assertEquals("Memory Resource", resource.getName());
        assertEquals("text/plain", resource.getMimeType());
        assertEquals("text/plain", resource.detectMimeType());
        assertEquals(22L, resource.length());
        assertTrue(resource.exists());
        assertEquals(Resource.Type.FILE, resource.getType());
        assertTrue(resource.isReadable());
        assertTrue(resource.isWritable());
        assertFalse(resource.isLocal());
        assertEquals("memory", resource.toURI().getScheme());
        assertEquals(-1, resource.toURI().getPort());
        assertEquals("This is a memory resource", resource.getDescription());
        assertEquals("txt", resource.getFileExtension());
        assertNotNull(resource.getWriter());
        assertNotNull(resource.getInputStream());
        assertEquals("text.txt", resource.getFileName());
        assertIterableEquals(Collections.EMPTY_LIST, resource.list());
        assertEquals(1597252114, resource.lastModified());
        assertArrayEquals(new byte[]{73, 32, 97, 109, 32, 119, 114, 105, 116, 105, 110, 103, 32, 106, 97, 118, 97, 32,
                99, 111, 100, 101}, resource.loadAsBytes());
        UserPasswordCredential credential = (UserPasswordCredential) resource.getCredential();
        assertEquals("username", credential.getUserName());
        assertEquals("password", credential.getPassword());
        assertEquals(Resource.NULL,resource.resolve(resource.getPath(), Resource.Type.FILE));
        assertEquals(Resource.NULL,resource.resolve(resource.getPath()));
        assertEquals(Resource.NULL,resource.get(resource.getPath()));
        assertEquals(Resource.NULL,resource.get(resource.getPath(), Resource.Type.FILE));
        assertEquals("f257484f158e1fb73e256d0fd84b1b12",resource.toHash());
    }


    @Test
    public void createFromBytes() throws IOException {
        Resource resource = addMemoryResourceProperties(MemoryResource.create(new byte[]{0, 1, 2, 3, 4, 5}));
        assertEquals("memory", resource.toURI().getScheme());
        assertEquals("Memory Resource", resource.getName());
        assertEquals("application/octet-stream", resource.getMimeType());
        assertEquals("application/octet-stream", resource.detectMimeType());
        assertEquals(6L, resource.length());
        assertTrue(resource.exists());
        assertEquals(Resource.Type.FILE, resource.getType());
        assertTrue(resource.isReadable());
        assertTrue(resource.isWritable());
        assertFalse(resource.isLocal());
        assertEquals("memory", resource.toURI().getScheme());
        assertEquals(-1, resource.toURI().getPort());
        assertEquals("This is a memory resource", resource.getDescription());
        assertNull(resource.getFileExtension());
        assertNotNull(resource.getWriter());
        assertNotNull(resource.getInputStream());
        assertNotNull(resource.getFileName());
        assertIterableEquals(Collections.EMPTY_LIST, resource.list());
        assertArrayEquals(new byte[]{0, 1, 2, 3, 4, 5}, resource.loadAsBytes());
        UserPasswordCredential credential = (UserPasswordCredential) resource.getCredential();
        assertEquals("username", credential.getUserName());
        assertEquals("password", credential.getPassword());
        assertEquals(Resource.NULL,resource.resolve(resource.getPath(), Resource.Type.FILE));
        assertEquals(Resource.NULL,resource.resolve(resource.getPath()));
        assertEquals(Resource.NULL,resource.get(resource.getPath()));
        assertEquals(Resource.NULL,resource.get(resource.getPath(), Resource.Type.FILE));
        assertEquals("fc091b4d2daed459ea8591aaccce88b2",resource.toHash());
    }

    @Test
    public void createFromBytesWithName() throws IOException {
        Resource resource = addMemoryResourceProperties(MemoryResource.create(new byte[]{0, 1, 2, 3, 4, 5},
                "data.bin"));
        assertEquals("memory", resource.toURI().getScheme());
        assertEquals("Memory Resource", resource.getName());
        assertEquals("application/octet-stream", resource.getMimeType());
        assertEquals("application/octet-stream", resource.detectMimeType());
        assertEquals(6L, resource.length());
        assertTrue(resource.exists());
        assertEquals(Resource.Type.FILE, resource.getType());
        assertTrue(resource.isReadable());
        assertTrue(resource.isWritable());
        assertFalse(resource.isLocal());
        assertEquals("memory", resource.toURI().getScheme());
        assertEquals(-1, resource.toURI().getPort());
        assertEquals("This is a memory resource", resource.getDescription());
        assertEquals("bin", resource.getFileExtension());
        assertNotNull(resource.getWriter());
        assertNotNull(resource.getInputStream());
        assertNotNull(resource.getFileName());
        assertIterableEquals(Collections.EMPTY_LIST, resource.list());
        assertArrayEquals(new byte[]{0, 1, 2, 3, 4, 5}, resource.loadAsBytes());
        UserPasswordCredential credential = (UserPasswordCredential) resource.getCredential();
        assertEquals("username", credential.getUserName());
        assertEquals("password", credential.getPassword());
        assertEquals(Resource.NULL,resource.resolve(resource.getPath(), Resource.Type.FILE));
        assertEquals(Resource.NULL,resource.resolve(resource.getPath()));
        assertEquals(Resource.NULL,resource.get(resource.getPath()));
        assertEquals(Resource.NULL,resource.get(resource.getPath(), Resource.Type.FILE));
        assertEquals("fc091b4d2daed459ea8591aaccce88b2",resource.toHash());
    }

    @Test
    public void createFromBytesWithNameWithLastModified() throws IOException {
        Resource resource = addMemoryResourceProperties(MemoryResource.create(new byte[]{0, 1, 2, 3, 4, 5},
                "data.bin", LAST_MODIFIED_TIME));
        assertEquals("memory", resource.toURI().getScheme());
        assertEquals("Memory Resource", resource.getName());
        assertEquals("application/octet-stream", resource.getMimeType());
        assertEquals("application/octet-stream", resource.detectMimeType());
        assertEquals(6L, resource.length());
        assertTrue(resource.exists());
        assertEquals(Resource.Type.FILE, resource.getType());
        assertTrue(resource.isReadable());
        assertTrue(resource.isWritable());
        assertFalse(resource.isLocal());
        assertEquals("memory", resource.toURI().getScheme());
        assertEquals(-1, resource.toURI().getPort());
        assertEquals("This is a memory resource", resource.getDescription());
        assertEquals("bin", resource.getFileExtension());
        assertNotNull(resource.getWriter());
        assertNotNull(resource.getInputStream());
        assertNotNull(resource.getFileName());
        assertIterableEquals(Collections.EMPTY_LIST, resource.list());
        assertArrayEquals(new byte[]{0, 1, 2, 3, 4, 5}, resource.loadAsBytes());
        UserPasswordCredential credential = (UserPasswordCredential) resource.getCredential();
        assertEquals("username", credential.getUserName());
        assertEquals("password", credential.getPassword());
        assertEquals(1597252114L,resource.lastModified());
        assertEquals(Resource.NULL,resource.resolve(resource.getPath(), Resource.Type.FILE));
        assertEquals(Resource.NULL,resource.resolve(resource.getPath()));
        assertEquals(Resource.NULL,resource.get(resource.getPath()));
        assertEquals(Resource.NULL,resource.get(resource.getPath(), Resource.Type.FILE));
        assertEquals("fc091b4d2daed459ea8591aaccce88b2",resource.toHash());
    }

    private Resource addMemoryResourceProperties(Resource resource) {
        return resource.withCredential(
                        new UserPasswordCredential("username", "password"))
                .withName("Memory Resource").withAttribute("key", "value")
                .withDescription("This is a memory resource");
    }

}