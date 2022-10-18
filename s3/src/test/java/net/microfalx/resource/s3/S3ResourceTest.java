package net.microfalx.resource.s3;

import net.microfalx.resource.Credential;
import net.microfalx.resource.StatefulResource;
import net.microfalx.resource.UserPasswordCredential;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;

import static net.microfalx.resource.ResourceUtils.addStartSlash;
import static net.microfalx.resource.ResourceUtils.removeStartSlash;

@Testcontainers
class S3ResourceTest {

    @Container
    private MinioContainer container = new MinioContainer();
    private Credential credential = new UserPasswordCredential("minio", "minio123");

    @Test
    void file() {
        StatefulResource file = S3Resource.file(create("file.txt"), credential);
        file.close();
    }

    @Test
    void directory() {
        StatefulResource directory = S3Resource.directory(create("file.txt"), credential);
        directory.close();
    }

    private URI create(String path) {
        return create("default", path);
    }

    private URI create(String bucket, String path) {
        return URI.create("s3://" + container.getHostAddress() + addStartSlash(bucket) + "/" + removeStartSlash(path));
    }

}