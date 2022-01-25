package net.tarau.resource;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertSame;

class ResourceFactoryTest {

    @Test
    void resolveFile() {
        Resource resource = ResourceFactory.resolve(URI.create("/tmp/file"));
        assertSame(FileResource.class, resource.getClass());
        resource = ResourceFactory.resolve(URI.create("file:/tmp/file"));
        assertSame(FileResource.class, resource.getClass());
    }

    @Test
    void resolveClassPath() {
        Resource resource = ResourceFactory.resolve(URI.create("classpath:/file1.txt"));
        assertSame(ClassPathResource.class, resource.getClass());
    }

    @Test
    void resolveUnknown() {
        Resource resource = ResourceFactory.resolve(URI.create("dummy:/file1.txt"));
        assertSame(NullResource.class, resource.getClass());
    }
}