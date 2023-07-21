package net.microfalx.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ClassPathResourceTest extends AbstractResourceTestCase {

    @Test
    void file() throws IOException {
        Resource file = ClassPathResource.file("file1.txt");
        assertTrue(file.exists());
        assertEquals(4, file.length());
        assertEquals("file1.txt", file.getPath());

        file = ClassPathResource.file("dir1/file11.txt");
        assertTrue(file.exists());
        assertEquals(4, file.length());
        assertEquals("dir1/file11.txt", file.getPath());
    }

    @Test
    void directory() throws IOException {
        assertTrue(ClassPathResource.directory("dir1").exists());
    }

    @Test
    void listSinglePackages() throws IOException {
        Resource resource = ClassPathResource.directory("org/junit/jupiter/api/parallel");
        assertTrue(resource.exists());
        assertEquals(7, resource.list().size());
    }

    @Test
    void listJarManifests() throws IOException {
        Resource resource = ClassPathResource.create("META-INF/MANIFEST.MF");
        assertTrue(resource.exists());
    }

    @Test
    void delete() throws IOException {
        assertThrows(IOException.class, () -> ClassPathResource.file("file1.txt").delete());
    }

    @Test
    void write() throws IOException {
        assertFalse(ClassPathResource.file("file1.txt").isWritable());
        assertThrows(IOException.class, () -> ClassPathResource.file("file1.txt").getOutputStream());
    }

    @Test
    void walk() throws IOException {
        Resource dir1 = ClassPathResource.directory("dir1");
        dir1.walk(visitor);
        assertCount(1, 0);
        dir1 = ClassPathResource.directory("dir3");
        dir1.walk(visitor, 1);
        assertCount(1, 2);
        dir1.walk(visitor, 2);
        assertCount(3, 3);
        dir1.walk(visitor, 3);
        assertCount(4, 3);
        dir1.walk(visitor);
        assertCount(4, 3);
    }
}