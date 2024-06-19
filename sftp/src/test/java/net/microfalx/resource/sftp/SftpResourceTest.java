package net.microfalx.resource.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import net.microfalx.resource.*;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.file.nativefs.NativeFileSystemFactory;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

import static net.microfalx.lang.IOUtils.appendStream;
import static net.microfalx.lang.IOUtils.getInputStreamAsBytes;
import static net.microfalx.lang.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

class SftpResourceTest {

    private static SshServer sshServer;
    private static final String userName = "dummy";
    private static final String password = "none";

    @BeforeAll
    static void startServer() {
        sshServer = setupServer();
    }

    @AfterAll
    static void stopServer() {
        try {
            if (sshServer != null) sshServer.close();
        } catch (IOException e) {
            // don't care
        }
    }

    @Test
    void createFile() throws IOException {
        Resource resource = SftpResource.file(createUri("file1.txt"), getCredential());
        assertNotNull(resource);
        assertEquals("sftp", resource.toURI().getScheme());
        assertEquals("localhost", resource.toURI().getHost());
        assertEquals("/file1.txt", resource.toURI().getPath());
        assertThrows(FileNotFoundException.class, resource::loadAsString);
        assertEquals("text/plain", resource.getMimeType());
        assertFalse(resource.exists());
        assertTrue(resource.isFile());
        assertTrue(resource.isReadable());
        assertTrue(resource.isWritable());
    }

    @Test
    void createDirectory() throws IOException {
        Resource resource = SftpResource.directory(createUri("dir"), getCredential());
        assertNotNull(resource);
        assertEquals("sftp", resource.toURI().getScheme());
        assertEquals("localhost", resource.toURI().getHost());
        assertEquals("/dir", resource.toURI().getPath());
        assertThrows(FileNotFoundException.class, resource::loadAsString);
        assertEquals("application/octet-stream", resource.getMimeType());
        assertFalse(resource.exists());
        assertTrue(resource.isDirectory());
        assertFalse(resource.isReadable());
        assertFalse(resource.isWritable());
    }

    @Test
    void create() throws IOException {
        Resource resource = SftpResource.create(createUri("file1.txt"), getCredential());
        assertNotNull(resource);
        assertEquals("sftp", resource.toURI().getScheme());
        assertEquals("localhost", resource.toURI().getHost());
        assertEquals("/file1.txt", resource.toURI().getPath());
        assertThrows(FileNotFoundException.class, resource::loadAsString);
        assertEquals("text/plain", resource.getMimeType());
        assertFalse(resource.exists());
        assertTrue(resource.isFile());
        assertTrue(resource.isReadable());
        assertTrue(resource.isWritable());
    }

    @Test
    void createWithType() throws IOException {
        Resource resource = SftpResource.create(Resource.Type.FILE, createUri("file1.txt"), getCredential());
        assertNotNull(resource);
        assertEquals("sftp", resource.toURI().getScheme());
        assertEquals("localhost", resource.toURI().getHost());
        assertEquals("/file1.txt", resource.toURI().getPath());
        assertThrows(FileNotFoundException.class, resource::loadAsString);
        assertEquals("text/plain", resource.getMimeType());
        assertFalse(resource.exists());
        assertTrue(resource.isFile());
        assertTrue(resource.isReadable());
        assertTrue(resource.isWritable());
    }

    @Test
    void getOutputStream() throws IOException {
        createFile("file.txt");
        Resource resource = SftpResource.create(createUri("file.txt"), getCredential());
        assertEquals("test", resource.loadAsString());
    }

    @Test
    void exists() throws IOException {
        Resource resource = SftpResource.create(createUri("missing.txt"), getCredential());
        assertFalse(resource.exists());
        createFile("file.txt");
        resource = SftpResource.create(createUri("file.txt"), getCredential());
        assertTrue(resource.exists());
    }

    @Test
    void lastModified() throws IOException {
        Resource resource = SftpResource.create(createUri("missing.txt"), getCredential());
        assertThrows(FileNotFoundException.class, () -> {
            getInputStreamAsBytes(resource.getInputStream());
        });
        createFile("file.txt");
        Resource resource2 = SftpResource.create(createUri("file.txt"), getCredential());
        assertTrue(resource2.lastModified() > 1642968529L);
    }

    @Test
    void length() throws IOException {
        Resource resource = SftpResource.create(createUri("missing.txt"), getCredential());
        assertThrows(FileNotFoundException.class, () -> getInputStreamAsBytes(resource.getInputStream()));
        createFile("file.txt");
        Resource resource2 = SftpResource.create(createUri("file.txt"), getCredential());
        assertEquals(4, resource2.length());
    }

    @Test
    void list() throws IOException {
        createFiles();
        Resource resource = SftpResource.create(createUri("list"), getCredential());
        assertEquals(4, resource.list().size());
    }

    @Test
    void resolve() throws IOException {
        Resource root = SftpResource.directory(createUri("directory"), getCredential());
        root.create();
        Resource resource = root.resolve("file3.txt").create();
        assertEquals("file3.txt", resource.getFileName());
        assertEquals("/directory/file3.txt", resource.getPath());
    }

    @Test
    void get() throws IOException {
        Resource root = SftpResource.directory(createUri("directory"), getCredential());
        root.create();
        Resource resource = root.get("/file3.txt", Resource.Type.FILE).create();
        assertEquals("file3.txt", resource.getFileName());
        assertEquals("/file3.txt", resource.getPath());
        assertThrows(ResourceException.class, () -> root.get("file3.txt", Resource.Type.FILE).create());
    }

    @Test
    void translateException(){
        Exception exception= new SftpException(ChannelSftp.OVERWRITE,"");
        SftpResource resource = (SftpResource) SftpResource.file(createUri("file1.txt"), getCredential());
        assertEquals(new IOException("SFTP action failed for '" + resource.toURI() + "'",resource.
                translateException(exception)).getMessage(),resource.translateException(exception).getMessage());
        exception= new SftpException(ChannelSftp.SSH_FX_PERMISSION_DENIED,"");
        assertEquals(new IOException("Permission denied for '" + resource.toURI() + "'").getMessage(),
                resource.translateException(exception).getMessage());
        exception= new JSchException();
        assertEquals(new IOException("SSH action failed for '" + resource.toURI() + "'",
                resource.translateException(exception)).getMessage(),resource.translateException(exception).getMessage());
        exception= new Exception();
        assertEquals( new IOException("Unknown failure for resource '" + resource.toURI() + "'",
                resource.translateException(exception)).getMessage(),resource.translateException(exception).getMessage());
    }

    private void createFile(String fileName) throws IOException {
        try (StatefulResource resource = SftpResource.create(createUri(fileName), getCredential())) {
            appendStream(resource.getWriter(), new StringReader("test"));
        }
    }

    private URI createUri(String path) {
        path = removeStartSlash(path);
        return URI.create("sftp://localhost:" + sshServer.getPort() + "/" + path);
    }

    private Credential getCredential() {
        return UserPasswordCredential.create(userName, password);
    }

    private void createFiles() throws IOException {
        Resource root = SftpResource.directory(createUri("list"), getCredential());
        root.create();
        root.resolve("dir1", Resource.Type.DIRECTORY).create();
        root.resolve("dir2", Resource.Type.DIRECTORY).create();
        root.resolve("file1.txt", Resource.Type.FILE).create();
        root.resolve("file2.txt", Resource.Type.FILE).create();
    }

    private static SshServer setupServer() {
        SshServer sshd = SshServer.setUpDefaultServer();
        int port = ThreadLocalRandom.current().nextInt(5_000) + 40_000;
        sshd.setPort(port);
        SftpSubsystemFactory factory = new SftpSubsystemFactory.Builder()
                .build();
        sshd.setSubsystemFactories(Collections.singletonList(factory));
        sshd.setPasswordAuthenticator(new PasswordAuthenticatorImpl());
        NativeFileSystemFactory fileSystemFactory = new NativeFileSystemFactory();
        fileSystemFactory.setUsersHomeDir(getTarget("sshd", true).getAbsolutePath());
        sshd.setFileSystemFactory(fileSystemFactory);
        sshd.setKeyPairProvider(createTestHostKeyProvider());
        try {
            sshd.start();
        } catch (IOException e) {
            throw new RuntimeException("Cannot start SSH server", e);
        }
        System.out.println("SSH Server started at " + sshd.getPort());
        return sshd;
    }

    public static KeyPairProvider createTestHostKeyProvider() {
        File file = getTarget("key.provider", false);
        SimpleGeneratorHostKeyProvider keyProvider = new SimpleGeneratorHostKeyProvider();
        keyProvider.setPath(file.toPath());
        keyProvider.setAlgorithm(KeyUtils.EC_ALGORITHM);
        keyProvider.setKeySize(256);
        return keyProvider;
    }

    private static File getTarget(String dirName, boolean directory) {
        String userDir = System.getProperty("user.dir");
        if (isEmpty(userDir)) userDir = "/tmp";
        File dir = new File(new File(new File(userDir), "target"), dirName);
        if (directory) dir.mkdirs();
        return dir;
    }

    static class PasswordAuthenticatorImpl implements PasswordAuthenticator {

        @Override
        public boolean authenticate(String userName, String password, ServerSession serverSession) throws PasswordChangeRequiredException, AsyncAuthException {
            return isNotEmpty(userName);
        }
    }
}