package net.microfalx.resource;

import net.microfalx.lang.StringUtils;
import net.microfalx.lang.UriUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilsTest {

    @Test
    void isRoot() {
        Resource resource = FileResource.file(new File("dir1" + File.separator + "file11.txt"));
        assertFalse(ResourceUtils.isRoot(resource.getPath()));
    }

    @Test
    void isFileUrl() {
        Resource resource = FileResource.file(new File("dir1" + File.separator + "file11.txt"));
        assertTrue(ResourceUtils.isFileUrl(resource.toURL()));
    }

    @Test
    void isFileUri() {
        Resource resource = FileResource.file(new File("dir1" + File.separator + "file11.txt"));
        assertTrue(ResourceUtils.isFileUri(resource.toURI()));
    }

    @Test
    void toFileUri() {
        Resource resource = FileResource.file(new File("dir1" + File.separator + "file11.txt"));
        assertEquals(resource.toURI(), ResourceUtils.toFileUri(resource.toURI()));
        assertEquals(URI.create("file://www.google.com/"), ResourceUtils.toFileUri(URI.create("https://www.google.com/")));
    }

    @Test
    void toDirectoryWithFile() {
        File file = new File("dir1" + File.separator + "file11.txt");
        assertEquals(new File(file.getAbsolutePath()), ResourceUtils.toDirectory(file));
    }

    @Test
    void toDirectoryWithFilePath() {
        File file = new File("dir1" + File.separator + "file11.txt");
        assertEquals(file.getPath() + File.separator, ResourceUtils.toDirectory(file.getPath()));
    }

    @Test
    void toDirectoryWithURI() throws URISyntaxException {
        assertEquals(UriUtils.appendPath("https://www.google.com", "/"),
                ResourceUtils.toDirectory(URI.create("https://www.google.com")));
    }

    @Test
    void isDirectory() {
        assertTrue(ResourceUtils.isDirectory("https://www.google.com/"));
        assertFalse(ResourceUtils.isDirectory("https://www.google.com"));
    }

    @Test
    void createName() {
        String text = "I am writing java code today.\n" +
                      "The code is suppose to see if everything works properly.\n";
        assertEquals(StringUtils.NA_STRING, ResourceUtils.createName(""));
        assertEquals("I am writing java code today....f everything works properly.",
                ResourceUtils.createName(text));
    }

    @Test
    void normalizeFileSystemPath() {
        assertEquals("dir1" + File.separator + "file11.txt",
                ResourceUtils.normalizeFileSystemPath("dir1/file11.txt"));
    }

    @Test
    void isDirectoryWithURI() throws IOException {
        Resource directoryResource=UrlResource.create(UriUtils.parseUri("https://www.google.com/"));
        assertTrue(ResourceUtils.isDirectory(directoryResource.toURI()));
        Resource fileResource=UrlResource.create(UriUtils.parseUri("https://www.google.com"));
        assertFalse(ResourceUtils.isDirectory(fileResource.toURI()));
    }

    @Test
    void isDirectoryWithURL() throws IOException {
        Resource directoryResource=UrlResource.create(UriUtils.parseUri("https://www.google.com/"));
        assertTrue(ResourceUtils.isDirectory(directoryResource.toURL()));
        Resource fileResource=UrlResource.create(UriUtils.parseUri("https://www.google.com"));
        assertFalse(ResourceUtils.isDirectory(fileResource.toURL()));
    }

    @Test
    void isDirectoryWithFileSystem() {
        File file= new File("src/test/resources/dir3/dir32/dir321");
        assertTrue(ResourceUtils.isDirectory(file, true));
        assertFalse(ResourceUtils.isDirectory(file, false));
        String directoryPath = ResourceUtils.toDirectory(file.getPath());
        assertTrue(ResourceUtils.isDirectory(new File(directoryPath),true));
        assertFalse(ResourceUtils.isDirectory(new File(directoryPath),false));
    }

    @Test
    void getTypeFromPathAndResourceType() {
        Resource resource = FileResource.file(new File("dir1" + File.separator + "file11.txt"));
        assertEquals(Resource.Type.DIRECTORY, ResourceUtils.getTypeFromPath("", Resource.Type.FILE));
        assertEquals(Resource.Type.FILE, ResourceUtils.getTypeFromPath(resource.getPath(), null));
        assertEquals(Resource.Type.DIRECTORY, ResourceUtils.getTypeFromPath(resource.getPath(), Resource.Type.DIRECTORY));
    }

    @Test
    void getTypeFromPath() {
        Resource resource = FileResource.file(new File("dir1" + File.separator + "file11.txt"));
        assertEquals(Resource.Type.DIRECTORY, ResourceUtils.getTypeFromPath(""));
        assertEquals(Resource.Type.FILE, ResourceUtils.getTypeFromPath(resource.getPath()));
    }

    @Test
    void appendResource() {
        Resource resource = FileResource.file(new File("dir1" + File.separator + "file11.txt"));
        List<Resource> resources = new ArrayList<>();
        ResourceUtils.appendResource(resources, resource, true);
        assertIterableEquals(List.of(resource), resources);
        CompositeResource compositeResource = new ClassPathResource.CompositeResource(Resource.Type.FILE, "dir3",
                resources);
        List<Resource> compositeResources = new ArrayList<>();
        ResourceUtils.appendResource(compositeResources, compositeResource, true);
        assertIterableEquals(List.of(resource), compositeResources);

    }

    @Test
    void hasSameContent() throws IOException {
        Resource firstResource = MemoryResource.create("text1");
        Resource secondResource = MemoryResource.create("text2");
        assertTrue(ResourceUtils.hasSameContent(null, null));
        assertFalse(ResourceUtils.hasSameContent(firstResource, null));
        assertFalse(ResourceUtils.hasSameContent(null, secondResource));
        assertFalse(ResourceUtils.hasSameContent(firstResource, secondResource));
        secondResource = MemoryResource.create("text1");
        assertTrue(ResourceUtils.hasSameContent(firstResource, secondResource));
    }

    @Test
    void throwUnsupported() {

    }

    @Test
    void toUri() {
        Resource resource = FileResource.directory(new File("dir3"));
        assertNull(ResourceUtils.toUri(""));
        assertEquals(UriUtils.parseUri(resource.toURI().toASCIIString()),
                ResourceUtils.toUri(resource.toURI().toASCIIString()));
    }

    @Test
    void hash() {
        String text = "I am writing java code";
        assertEquals("13d549740a4f94df8c5489eadb5075b7", ResourceUtils.hash(text));
    }

    @Test
    void getDepth() {
        File file = new File("dir3" + File.separator + "dir32" + File.separator + "dir321" + File.separator +
                "file3211.txt");
        assertEquals(4, ResourceUtils.getDepth(file.getPath()));
    }

    @Test
    void retryWithStatus() {
        Resource resource = FileResource.file(new File("dir3/dir32/dir321/file3211.txt"));
        assertTrue(ResourceUtils.retryWithStatus(resource, Resource::isAbsolutePath));
    }

    @Test
    void retryWithException() {
        Resource resource = FileResource.file(new File("dir3/dir32/dir321/file3211.txt"));
        assertTrue(ResourceUtils.retryWithException(resource, Resource::isAbsolutePath));
    }
}