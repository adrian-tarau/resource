package net.microfalx.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class UrlResourceTest {

    @Test
    void createFromURI() throws IOException {
        Resource resource = UrlResource.create(ClassPathResource.file("file11.txt").toURI());
        assertNotNull(resource);
    }

    @Test
    void createFromURL() throws MalformedURLException {
        Resource resource = UrlResource.create(ClassPathResource.file("file11.txt").toURI().toURL());
        assertNotNull(resource);
    }

    @Test
    void createFromURLAndType() throws MalformedURLException {
        Resource resource = UrlResource.create(ClassPathResource.directory("dir1").toURI().toURL(),
                Resource.Type.DIRECTORY);
        assertNotNull(resource);
    }

    @Test
    void getParent() throws IOException {
        UrlResource resource = (UrlResource) UrlResource.create(URI.
                create("http://example.com/software/htp/cics/index.html"));
        assertEquals("/software/htp/cics", resource.getParent().getPath());
    }

    @Test
    void getInputStream() throws IOException {
        UrlResource resource = (UrlResource) UrlResource.create(URI.
                create("https://www.google.com/"));
        assertNotNull(resource.getInputStream());
    }

    @Test
    void doExists() throws IOException {
        UrlResource resource = (UrlResource) UrlResource.create(URI.
                create("http://example.com/software/htp/cics/index.html"));
        assertFalse(resource.doExists());
    }


    @Test
    void getFileName() throws IOException {
        UrlResource resource = (UrlResource) UrlResource.create(URI.
                create("http://example.com/software/htp/cics/index.html"));
        assertEquals("index.html", resource.getFileName());
        assertEquals("index.html", resource.getFileName());
    }


    @Test
    void list() throws IOException {
        Resource resource = ClassPathResource.directory("dir3");
        assertEquals(3, resource.list().size());
        UrlResource urlResource = (UrlResource) UrlResource.create(URI.
                create("http://example.com/software/htp/cics/index.html"));
        assertThrows(IllegalStateException.class, urlResource::doList);
    }

    @Test
    void resolve() throws IOException {
        UrlResource resource = (UrlResource) UrlResource.create(URI.
                create("http://example.com/software/htp/cics/"));
        assertEquals("index.html", resource.resolve("index.html").getFileName());
        assertEquals("/software/htp/cics/index.html", resource.resolve("index.html", Resource.Type.FILE).getPath());
    }

    @Test
    void resolveWithType() throws IOException {
        UrlResource resource = (UrlResource) UrlResource.create(URI.
                create("http://example.com/software/htp/cics/"));
        assertEquals("index.html", resource.resolve("index.html", Resource.Type.FILE).getFileName());
        assertEquals("/software/htp/cics/index.html", resource.resolve("index.html", Resource.Type.FILE).getPath());
    }

    @Test
    void get() throws IOException {
        UrlResource resource = (UrlResource) UrlResource.create(URI.
                create("http://example.com/software/htp/cics/"));
        assertEquals("index.html", resource.get("index.html", Resource.Type.FILE).getFileName());
        assertEquals("/index.html", resource.get("index.html", Resource.Type.FILE).getPath());

    }

    @Test
    void lastModified() throws IOException {
        UrlResource resource = (UrlResource) UrlResource.create(URI.
                create("http://example.com/software/htp/cics/index.html"));
        assertEquals(0L, resource.lastModified());
    }

    @Test
    void doLength() throws IOException {
        UrlResource resource = (UrlResource) UrlResource.create(URI.
                create("https://www.google.com/"));
        assertEquals(-1, resource.doLength());
        resource = (UrlResource) UrlResource.create(URI.
                create("https://localhost/test"));
        assertEquals(-1, resource.doLength());
    }

    @Test
    void toURI() {
        Resource resource = ClassPathResource.file("dir1/file11.txt");
        assertTrue(resource.toURI().toASCIIString().endsWith("file11.txt"));
    }


    @Test
    void createFromUriString() throws IOException {
        UrlResource resource = (UrlResource) UrlResource.create(URI.
                create("http://example.com/software/htp/cics/index.html"));
        Resource newUriResource = resource.createFromUriString("http://example.com/software/htp/cics/home.html",
                Resource.Type.FILE);
        assertEquals("/software/htp/cics/home.html", newUriResource.getPath());
    }
}