package net.microfalx.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MemoryResourceTest {

    @Test
    public void fromBytes() {
        Resource resource = MemoryResource.create(new byte[]{0, 1, 2, 3, 4, 5});
        assertEquals("memory", resource.toURI().getScheme());
        assertNotNull("memory", resource.getPath());
        assertNotNull("memory", resource.getPath());
    }

    @Test
    public void fromBytesWithName() {
        Resource resource = MemoryResource.create(new byte[]{0, 1, 2, 3, 4, 5}, "data.bin");
        assertEquals("memory", resource.toURI().getScheme());
        assertEquals("data.bin", resource.getName());
        assertEquals("/data.bin", resource.getPath());
    }

}