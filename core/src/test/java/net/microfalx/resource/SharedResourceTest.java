package net.microfalx.resource;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import static net.microfalx.lang.IOUtils.getInputStreamAsBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SharedResourceTest {

    @BeforeEach
    void before() {
        ResourceFactory.setRoot(ClassPathResource.file("dir3").toFile());
    }

    @Test
    void noRootResource() throws IOException {
        ResourceFactory.setRoot(null);
        Assertions.assertThatThrownBy(() -> SharedResource.file("test").length()).hasMessageContaining("root of the shared");
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
        AtomicInteger fileCount = new AtomicInteger();
        AtomicInteger directoryCount = new AtomicInteger();
        dir1.walk((root, child) -> {
            if (child.isFile()) {
                fileCount.incrementAndGet();
            } else {
                directoryCount.incrementAndGet();
            }
            return true;
        });
        assertEquals(4, fileCount.get());
        assertEquals(3, directoryCount.get());
    }

    @Test
    void toUri() {
        assertEquals(URI.create("shared:/test1"), SharedResource.file("test1").toURI());

    }

    private Resource fromFile(String path) {
        return ClassPathResource.file(path).toFile();
    }

}