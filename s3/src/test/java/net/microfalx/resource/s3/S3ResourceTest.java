package net.microfalx.resource.s3;

import net.microfalx.lang.StringUtils;
import net.microfalx.lang.UriUtils;
import net.microfalx.resource.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import static net.microfalx.lang.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * It needs docker. Docker in WSL2 cannot be accessed.
 * Start a MinIO in container with
 * docker run -d -p 9000:9000 -p 9001:9001 --name minio -e MINIO_ACCESS_KEY=minio -e MINIO_SECRET_KEY=minio123 minio/minio server /data --console-address ":9001"
 */
class S3ResourceTest {

    //@Container
    //private MinioContainer container = new MinioContainer();
    private Credential credential;
    private static final String bucket = "test";

    @BeforeEach
    void setup() {
        String minioEndpoint = System.getenv("MINIO_ENDPOINT");
        String minioAccessKey = StringUtils.defaultIfEmpty(System.getenv("MINIO_ACCESS_KEY"), "minio");
        String minioSecretKey = System.getenv("MINIO_SECRET_KEY");
        if (StringUtils.isEmpty(minioEndpoint)) throw new IllegalStateException("MINIO_ENDPOINT not set");
        if (StringUtils.isEmpty(minioSecretKey)) throw new IllegalStateException("MINIO_SECRET_KEY not set");
        credential = new UserPasswordCredential(minioAccessKey, minioSecretKey);
        URL endpoint = UriUtils.parseUrl(minioEndpoint);
        S3Resource.setDefaultEndpoint(endpoint);
    }

    @Test
    void createFile() throws IOException {
        S3Resource file = upload("file1.txt", ClassPathResource.file("s3/file1.txt"));
        assertEquals("file1.txt", file.getFileName());
        assertEquals(4, file.length());
        assertNotNull(file.getEtag());
        assertNull(file.getOwner());
        assertTrue(file.lastModified() > 0);
        assertEquals("test", file.loadAsString());
        file.close();
    }

    @Test
    void listDirectory() throws IOException {
        uploadDirectory(EMPTY_STRING);
        StatefulResource directory = S3Resource.directory(create(""), credential);
        AtomicInteger fileCount = new AtomicInteger();
        AtomicInteger directoryCount = new AtomicInteger();
        for (Resource resource : directory.list()) {
            if (resource.isFile()) {
                fileCount.incrementAndGet();
            } else {
                directoryCount.incrementAndGet();
            }
        }
        assertEquals(1, fileCount.get());
        assertEquals(2, directoryCount.get());
        directory.close();
    }

    @Test
    void walkDirectory() throws IOException {
        uploadDirectory(EMPTY_STRING);
        StatefulResource directory = S3Resource.directory(create(EMPTY_STRING), credential);
        AtomicInteger fileCount = new AtomicInteger();
        AtomicInteger directoryCount = new AtomicInteger();
        directory.walk((root, child) -> {
            if (child.isFile()) {
                fileCount.incrementAndGet();
            } else {
                directoryCount.incrementAndGet();
            }
            return true;
        });
        assertEquals(5, fileCount.get());
        assertEquals(0, directoryCount.get());
        directory.close();
    }

    @Test
    void removeDirectory() throws IOException {
        StatefulResource directory = S3Resource.directory(create(EMPTY_STRING), credential);
        directory.delete();
        directory.close();
    }

    private URI create(String path) {
        return create(bucket, path);
    }

    private void uploadDirectory(String target) throws IOException {
        StatefulResource directory = S3Resource.directory(create(target), credential);
        directory.copyFrom(ClassPathResource.directory("s3"));
        directory.close();
    }

    private S3Resource upload(String target, Resource source) throws IOException {
        S3Resource file = (S3Resource) S3Resource.file(create(target), credential);
        file.delete();
        assertFalse(file.exists());
        file.copyFrom(source);
        assertTrue(file.exists());
        assertEquals(4, file.length());
        return file;
    }

    private URI create(String bucket, String path) {
        path = isNotEmpty(path) ? "/" + removeStartSlash(path) : EMPTY_STRING;
        return URI.create("s3:/" + removeStartSlash(bucket) + path);
        //return URI.create("s3://" + container.getHostAddress() + addStartSlash(bucket) + "/" + removeStartSlash(path));
    }

}