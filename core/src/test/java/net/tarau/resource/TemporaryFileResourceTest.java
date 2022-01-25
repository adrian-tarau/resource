package net.tarau.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporaryFileResourceTest {

    @Test
    void file() {
        assertEquals("fixed.txt", TemporaryFileResource.file("fixed.txt").getFileName());
        assertEquals(Resource.Type.FILE, TemporaryFileResource.file("fixed.txt").getType());
        assertTrue(TemporaryFileResource.file("prefix", "suffix").getFileName().startsWith("prefix"));
    }

    @Test
    void directory() {
        assertEquals("fixed.txt", TemporaryFileResource.directory("fixed.txt").getFileName());
        assertEquals(Resource.Type.DIRECTORY, TemporaryFileResource.directory("fixed.txt").getType());
        assertTrue(TemporaryFileResource.directory("prefix", "suffix").getFileName().startsWith("prefix"));
    }
}