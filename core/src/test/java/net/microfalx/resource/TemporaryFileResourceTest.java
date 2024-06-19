package net.microfalx.resource;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class TemporaryFileResourceTest {

    @Test
    void createFileWithPrefixAndSuffix() throws IOException {
        Resource resource = TemporaryFileResource.file("test", "file");
        Assertions.assertThat(resource.getFileName()).startsWith("test").endsWith("file");
        assertNull(resource.getFileExtension());
        assertEquals("application/octet-stream", resource.getMimeType());
        assertTrue(resource.isFile());
        assertFalse(resource.exists());
        assertFalse(resource.isDirectory());
        Assertions.assertThat(resource.getPath()).endsWith("file");
    }

    @Test
    void createDirectoryWithPrefixAndSuffix() throws IOException {
        Resource resource = TemporaryFileResource.directory("test", "directory");
        Assertions.assertThat(resource.getFileName()).startsWith("test").endsWith("directory");
        assertNull(resource.getFileExtension());
        assertEquals("application/octet-stream", resource.getMimeType());
        assertTrue(resource.isDirectory());
        assertFalse(resource.exists());
        assertFalse(resource.isFile());
        Assertions.assertThat(resource.getPath()).endsWith("directory");
    }

    @Test
    void createFileWithFileName() {
        assertEquals("fixed.txt", TemporaryFileResource.file("fixed.txt").getFileName());
        assertEquals(Resource.Type.FILE, TemporaryFileResource.file("fixed.txt").getType());
        assertTrue(TemporaryFileResource.file("prefix", "suffix").getFileName().startsWith("prefix"));
    }

    @Test
    void createDirectoryWithDirectoryName() {
        assertEquals("fixed.txt", TemporaryFileResource.directory("fixed.txt").getFileName());
        assertEquals(Resource.Type.DIRECTORY, TemporaryFileResource.directory("fixed.txt").getType());
        assertTrue(TemporaryFileResource.directory("prefix", "suffix").getFileName().startsWith("prefix"));
    }

    @Test
    void createFileWithTypeAndFileName() throws IOException {
        Resource resource = TemporaryFileResource.create(Resource.Type.FILE, "test");
        assertEquals("test", resource.getFileName());
        assertEquals("application/octet-stream", resource.getMimeType());
        assertTrue(resource.isFile());
        assertFalse(resource.exists());
        assertFalse(resource.isDirectory());
        Assertions.assertThat(resource.getPath()).contains("test");
    }
}