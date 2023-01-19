package net.microfalx.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FileResourceTest {

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

    private Resource fromFile(String path) {
        return ClassPathResource.file(path).toFile();
    }

    private Resource fromDirectory(String path) {
        return ClassPathResource.directory(path).toFile();
    }

}