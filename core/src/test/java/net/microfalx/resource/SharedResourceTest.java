package net.microfalx.resource;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static net.microfalx.lang.IOUtils.getInputStreamAsBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;

class lSharedResourceTest extends AbstractResourceTestCase {

    @BeforeEach
    void before() {
        ResourceFactory.setShared(ClassPathResource.file("dir3").toFile());
    }

    @Test
    void getPath() throws IOException {
        assertEquals("/file31.txt", SharedResource.file("file31.txt").getPath());
        assertEquals("/dir31", SharedResource.directory("dir31").getPath());
    }

    @Test
    void length() throws IOException {
        assertEquals(4, SharedResource.file("file31.txt").length());
    }

    @Test
    void lastModified() throws IOException {
        Assertions.assertThat(SharedResource.file("file31.txt").lastModified()).isGreaterThan(1689515333996L);
    }

    @Test
    void getInputStream() throws IOException {
        assertEquals(4, getInputStreamAsBytes(SharedResource.file("file31.txt").getInputStream()).length);
    }

    @Test
    void list() throws IOException {
        Resource dir1 = SharedResource.directory("/");
        assertEquals(3, dir1.list().size());
    }

    @Test
    void walk() throws IOException {
        Resource dir1 = SharedResource.directory("/");
        dir1.walk(visitor);
        assertCount(4, 3);
    }

    @Test
    void toUri() {
        assertEquals(URI.create("shared:/test1"), SharedResource.file("test1").toURI());

    }

}