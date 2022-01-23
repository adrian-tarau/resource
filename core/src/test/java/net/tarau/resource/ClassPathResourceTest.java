package net.tarau.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassPathResourceTest {

    @Test
    void file() {
        assertTrue(ClassPathResource.file("file1.txt").exists());
    }

    @Test
    void directory() {
        assertTrue(ClassPathResource.directory("dir1").exists());
    }

    @Test
    void listSinglePackages() {
        Resource resource = ClassPathResource.directory("org/junit/jupiter/api/parallel");
        assertTrue(resource.exists());
        assertEquals(7, resource.list().size());
    }

    @Test
    void listJarManifests() {
        Resource resource = ClassPathResource.create("META-INF/MANIFEST.MF");
        assertTrue(resource.exists());
    }
}