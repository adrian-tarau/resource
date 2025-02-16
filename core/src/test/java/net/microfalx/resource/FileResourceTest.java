package net.microfalx.resource;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class FileResourceTest extends AbstractResourceTestCase {

    @Test
    void createFromURI() {
        Resource resource = FileResource.create(URI.create("dir1/file11.txt"));
        assertNotNull(resource);
    }

    @Test
    void createWithURIAndType() {
        Resource resource = FileResource.create(URI.create("dir1/file11.txt"), Resource.Type.FILE);
        assertNotNull(resource);
    }

    @Test
    void createFromFile() {
        Resource resource = FileResource.create(new File("dir1/file11.txt"));
        assertNotNull(resource);
    }

    @Test
    void file() throws IOException {
        Resource file = fromFile("file1.txt");
        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertFalse(file.isDirectory());
        assertNotEquals(0, file.lastModified());
        assertEquals("file1.txt", file.getFileName());
    }

    @Test
    void directory() throws IOException {
        Resource directory = FileResource.directory(new File("src/test/resources/dir1"));
        assertTrue(directory.exists());
        assertFalse(directory.isFile());
        assertTrue(directory.isDirectory());
    }

    @Test
    void createFromResource() throws IOException {
        Resource resource = FileResource.create(MemoryResource.create("I am writing java code"));
        assertNotNull(resource);
        resource = FileResource.create(FileResource.directory(new File("src/test/resources/dir1")));
        assertEquals(resource, FileResource.directory(new File("src/test/resources/dir1")));
    }

    @Test
    void getParent() {
        Resource resource = FileResource.file(new File("dir3/dir32/dir321/file3211.txt"));
        assertTrue(resource.getParent().isAbsolutePath());
    }

    @Test
    void getFile() {
        FileResource resource = (FileResource) FileResource.file(new File("dir3/dir32/dir321/file3211.txt"));
        assertEquals("file3211.txt", resource.getFile().getName());
        assertEquals("dir3" + File.separator + "dir32" + File.separator + "dir321" + File.separator +
                "file3211.txt", resource.getFile().getPath());
    }

    @Test
    void getInputStream() throws IOException {
        Resource file = fromFile("file1.txt");
        assertNotNull(file.getInputStream());
    }

    @Test
    void getOutputStream() throws IOException {
        Resource file = fromFile("file1.txt");
        assertNotNull(file.getOutputStream());
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

    @Test
    void resolve() {
        Resource resource = FileResource.directory(new File("dir1"));
        resource = resource.resolve("file11.txt");
        Assertions.assertThat(resource.getPath()).endsWith("core/dir1/file11.txt");
    }

    @Test
    void resolveWithType() {
        Resource resource = FileResource.directory(new File("dir1"));
        resource = resource.resolve("file11.txt", Resource.Type.FILE);
        Assertions.assertThat(resource.getPath()).endsWith("core/dir1/file11.txt");
    }

    @Test
    void get() {
        Resource resource = FileResource.directory(new File("dir1"));
        resource = resource.get("file11.txt");
        Assertions.assertThat(resource.getPath()).endsWith("core/file11.txt");
    }

    @Test
    void getWithType() {
        Resource resource = FileResource.directory(new File("dir1"));
        resource = resource.get("file11.txt", Resource.Type.FILE);
        Assertions.assertThat(resource.getPath()).endsWith("core/file11.txt");
    }

    @Test
    void create() throws IOException {
        Resource resource = FileResource.directory(new File("file3.txt"));
        assertEquals("file3.txt", resource.create().getFileName());
        Assertions.assertThat(resource.create().getPath()).endsWith("core/file3.txt/");
    }

    @Test
    void delete() throws IOException {
        Resource resource = FileResource.file(new File("file3.txt"));
        assertFalse(resource.delete().exists());
    }

    @Test
    void toFile() {
        Resource resource = FileResource.directory(new File("dir1"));
        assertNotNull(resource.toFile());
    }

    @Test
    void toHash() {
        Resource resource = FileResource.file(new File("file3.txt"));
        assertEquals("57423e12251e2f60a7cf3c3c3b319657", resource.toHash());
        assertEquals("1c7690a1699c2191f59555920c2d28ff", resource.withAttribute(Resource.PATH_ATTR, "path1").toHash());
        assertEquals("c898172e147b1adca7d6617a279b7c22", resource.withAttribute(Resource.HASH_ATTR, "xxxxxx").toHash());
        assertEquals("d95d7abdf7f633860099814ecf9b2119", resource.withAttribute(Resource.HASH_ATTR, "xxxxxx").withAttribute(Resource.PATH_ATTR, "path1").toHash());
    }

    @Test
    void supports() {
        FileResource.FileResourceResolver fileResourceResolver = new FileResource.FileResourceResolver();
        assertFalse(fileResourceResolver.supports(URI.create("https://www.google.com/")));
    }

    @Test
    void resolveWithURIAndType() {
        FileResource.FileResourceResolver fileResourceResolver = new FileResource.FileResourceResolver();
        Resource resolveResource = fileResourceResolver.resolve(URI.
                create("http://example.com/software/htp/cics/index.html"), Resource.Type.FILE);
        assertEquals("index.html", resolveResource.getFileName());
        assertTrue(resolveResource.isFile());
        Assertions.assertThat(resolveResource.getPath()).endsWith("/software/htp/cics/index.html");
    }

}