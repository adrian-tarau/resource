package net.tarau.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassPathResourceTest {

    @Test
    void file() throws IOException {
        assertTrue(ClassPathResource.file("file1.txt").exists());
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
}