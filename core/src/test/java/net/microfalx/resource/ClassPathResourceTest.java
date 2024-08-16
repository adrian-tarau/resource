package net.microfalx.resource;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

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
    void create() throws IOException {
        Resource resource = ClassPathResource.create("META-INF/MANIFEST.MF");
        assertTrue(resource.exists());
    }

    @Test
    void createFile() throws IOException {
        Resource resource = ClassPathResource.create("dir1/file11.txt", Resource.Type.FILE);
        assertTrue(resource.exists());
    }

    @Test
    void createDirectory() throws IOException {
        Resource resource = ClassPathResource.create("dir1", Resource.Type.DIRECTORY);
        assertTrue(resource.exists());
    }

    @Test
    void files() throws IOException {
        Resource resource = ClassPathResource.files("dir1");
        assertTrue(resource.exists());
        assertEquals(1,resource.list().size());
    }

    @Test
    void resolve() throws IOException {
        Resource resource = new ClassPathResource(Resource.Type.DIRECTORY, new File("dir1").toURI().toURL(),
                "dir1");
        assertTrue(resource.resolve("file11.txt").exists());
        assertEquals("dir1/file11.txt",resource.resolve("file11.txt").getPath());
    }

    @Test
    void resolveFile() throws IOException {
        Resource resource = new ClassPathResource(Resource.Type.DIRECTORY, new File("dir1").toURI().toURL(),
                "dir1");
        assertTrue(resource.resolve("file11.txt", Resource.Type.FILE).exists());
        assertEquals("dir1/file11.txt",resource.resolve("file11.txt", Resource.Type.FILE).getPath());
    }

    @Test
    void get() throws IOException {
        Resource resource = new ClassPathResource(Resource.Type.DIRECTORY, new File("dir1").toURI().toURL(),
                "dir1");
        assertTrue(resource.get("dir1").exists());
        assertEquals("dir1",resource.get("dir1").getPath());
    }

    @Test
    void getFile() throws IOException {
        Resource resource = new ClassPathResource(Resource.Type.FILE, new File("dir1/file11.txt").toURI().toURL(),
                "dir1/file11.txt");
        assertTrue(resource.get("dir1/file11.txt", Resource.Type.FILE).exists());
        assertEquals("dir1/file11.txt",resource.get("dir1/file11.txt", Resource.Type.FILE).getPath());
    }

    @Test
    void delete() {
        assertThrows(IOException.class, () -> ClassPathResource.file("file1.txt").delete());
    }

    @Test
    void write() {
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

    @Test
    void createCompositeResource() throws IOException {
        Resource childResource= ClassPathResource.file("dir1/file11.txt");
        Resource resource= new ClassPathResource.CompositeResource(Resource.Type.DIRECTORY,"dir1",
                Collections.singletonList(childResource));
        assertEquals("",resource.getParent().getPath());
        assertEquals("dir1",resource.getFileName());
        assertEquals(1718719881354L,resource.lastModified());
        assertEquals(4,resource.length());
        assertEquals("file:/C:/Projects/opensource/resource/core/target/test-classes/dir1/file11.txt",
                resource.toURI().toASCIIString());
        assertIterableEquals(Collections.singletonList(childResource),resource.list());
        assertThrows(IllegalStateException.class, resource::getInputStream);
        ClassPathResource.CompositeResource compositeResource= (ClassPathResource.CompositeResource) resource;
        assertIterableEquals(Collections.singletonList(childResource),compositeResource.getResources());
    }

    @Test
    void createClassPathResourceResolver() throws IOException {
        ClassPathResource.ClassPathResourceResolver classPathResourceResolver=
                new ClassPathResource.ClassPathResourceResolver();
        Resource resource = new ClassPathResource(Resource.Type.DIRECTORY, new File("dir1").toURI().toURL(),
                "dir1");
        assertFalse(classPathResourceResolver.supports(resource.toURI()));
        assertEquals("/dev/null",classPathResourceResolver.resolve(resource.toURI(), Resource.Type.DIRECTORY).getPath());
    }
}