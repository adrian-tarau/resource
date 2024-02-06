package net.microfalx.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FileResourceTest extends AbstractResourceTestCase {

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
        assertTrue(directory.length()> 0);
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

}