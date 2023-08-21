package net.microfalx.resource;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ResourceFactoryTest {

    @Test
    void resolveFile() {
        Resource resource = ResourceFactory.resolve(URI.create("/tmp/file"));
        assertSame(FileResource.class, resource.getClass());
        resource = ResourceFactory.resolve(URI.create("file:/tmp/file"));
        assertSame(FileResource.class, resource.getClass());
        ResourceFactory.resolve(System.getProperty("user.home"));
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

    @Test
    void mimeType() {
        assertEquals(MimeType.APPLICATION_OCTET_STREAM.toString(), ResourceFactory.detect(new ByteArrayInputStream(new byte[100]), "demo.bin"));
        assertEquals(MimeType.TEXT_PLAIN.toString(), ResourceFactory.detect(new ByteArrayInputStream("Text".getBytes()), "demo.bin"));
        assertEquals(MimeType.TEXT_PLAIN.toString(), ResourceFactory.detect(new ByteArrayInputStream(new byte[100]), "demo.txt"));
        assertEquals(MimeType.TEXT_HTML.toString(), ResourceFactory.detect(new ByteArrayInputStream(new byte[100]), "demo.html"));
    }
}