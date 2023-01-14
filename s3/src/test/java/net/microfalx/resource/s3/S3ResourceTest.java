package net.microfalx.resource.s3;

import net.microfalx.resource.Credential;
import net.microfalx.resource.StatefulResource;
import net.microfalx.resource.UserPasswordCredential;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;

import static net.microfalx.resource.ResourceUtils.addStartSlash;
import static net.microfalx.resource.ResourceUtils.removeStartSlash;

/**
 * It needs docker. Docker in WSL2 cannot be accessed.
 * Start a MinIO in container with
 * docker run -d -p 9000:9000 -p 9001:9001 --name minio -e MINIO_ACCESS_KEY=minio -e MINIO_SECRET_KEY=minio123 minio/minio server /data --console-address ":9001"
 */
@Testcontainers
class S3ResourceTest {

    //@Container
    //private MinioContainer container = new MinioContainer();
    private Credential credential = new UserPasswordCredential("minio", "minio123");
    private static final String bucket = "test" + Long.toHexString(System.currentTimeMillis());

    @Test
    void file() {
        StatefulResource file = S3Resource.file(create("file.txt"), credential);
        file.close();
    }

    @Test
    void directory() {
        StatefulResource directory = S3Resource.directory(create("dir1"), credential);
        directory.close();
    }

    private URI create(String path) {
        return create(bucket, path);
    }

    private URI create(String bucket, String path) {
        return URI.create("s3://localhost:9000" + addStartSlash(bucket) + "/" + removeStartSlash(path));
        //return URI.create("s3://" + container.getHostAddress() + addStartSlash(bucket) + "/" + removeStartSlash(path));
    }

}