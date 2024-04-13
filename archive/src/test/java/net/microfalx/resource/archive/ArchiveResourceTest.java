package net.microfalx.resource.archive;

import net.microfalx.lang.IOUtils;
import net.microfalx.resource.ClassPathResource;
import net.microfalx.resource.Resource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchiveResourceTest {

    @Test
    void getArchiveType() {
        assertEquals(ArchiveResource.Type.BZIP2, fromFile("file1.txt.bz2").getArchiveType());
        assertEquals(ArchiveResource.Type.GZIP, fromFile("file1.txt.gz").getArchiveType());
        assertEquals(ArchiveResource.Type.SEVEN_Z, fromFile("sample.7z").getArchiveType());
        assertEquals(ArchiveResource.Type.TAR, fromFile("sample.tar").getArchiveType());
        assertEquals(ArchiveResource.Type.GZIP, fromFile("sample.tar.gz").getArchiveType());
        assertEquals(ArchiveResource.Type.ZIP, fromFile("sample.zip").getArchiveType());
    }

    @Test
    void fromFileName() {
        assertEquals(ArchiveResource.Type.BZIP2, ArchiveResource.fromExtension(ClassPathResource.file("file1.txt.bz2").toURI()));
        assertEquals(ArchiveResource.Type.SEVEN_Z, ArchiveResource.fromExtension(fromFile("sample.7z")));
    }

    @Test
    void fromZipFileWithReference() throws IOException {
        Resource resource = fromFile("sample.zip").withFragment("file1.txt");
        assertEquals("file1.txt", resource.getFileName());
        assertEquals("Test", resource.loadAsString());
    }

    @Test
    void fromTarFileWithReference() throws IOException {
        Resource resource = fromFile("sample.tar").withFragment("file1.txt");
        assertEquals("file1.txt", resource.getFileName());
        assertEquals("Test", resource.loadAsString());
    }

    @Test
    void getContent() throws IOException {
        assertEquals(4, fromFile("file1.txt.bz2").loadAsBytes().length);
        assertEquals(1596, fromFile("sample.zip").loadAsBytes().length);
    }

    @Test
    void walkCompressed() throws IOException {
        AtomicInteger counter = new AtomicInteger();
        fromFile("file1.txt.bz2").walk((parent, child) -> {
            counter.incrementAndGet();
            return true;
        });
        assertEquals(0, counter.get());
    }

    @Test
    void list() throws IOException {
        assertEquals(5, fromFile("sample.zip").list().size());
    }

    @Test
    void walkArchive() throws IOException {
        AtomicInteger fileCount = new AtomicInteger();
        AtomicInteger fileSize = new AtomicInteger();
        AtomicInteger directoryCount = new AtomicInteger();
        fromFile("sample.zip").walk((parent, child) -> {
            assertTrue(child.isFile() ? child.length() > 0 : child.length() == 0);
            if (child.isFile()) fileSize.addAndGet(IOUtils.getInputStreamAsBytes(child.getInputStream()).length);
            assertTrue(child.lastModified() > 0);
            if (child.isFile()) {
                fileCount.incrementAndGet();
            } else {
                directoryCount.incrementAndGet();
            }
            return true;
        });
        assertEquals(11, fileCount.get());
        assertEquals(44, fileSize.get());
        assertEquals(4, directoryCount.get());
    }

    private ArchiveResource fromFile(String path) {
        return (ArchiveResource) ArchiveResource.create(ClassPathResource.file(path).toFile());
    }

}