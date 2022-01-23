package net.tarau.resource.sftp;

import net.tarau.resource.*;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Collections;

import static net.tarau.resource.ResourceUtils.isNotEmpty;
import static org.junit.jupiter.api.Assertions.*;

class SftpResourceTest {

    private static SshServer sshServer;
    private static final String userName = "dummy";
    private static final String password = "none";

    @BeforeAll
    static void startServer() {
        sshServer = setupServer();
    }

    @BeforeAll
    static void stopServer() {
        try {
            if (sshServer != null) sshServer.close();
        } catch (IOException e) {
            // don't care
        }
    }

    @Test
    void create() {
        Resource resource = SftpResource.create(createUri("file1.txt"), getCredential());
        assertNotNull(resource);
        assertEquals("sftp://tarau.net/file1.txt", resource.toURI().toASCIIString());
    }

    @Test
    void getInputStreamWithMissingFile() throws IOException {
        Resource resource = SftpResource.create(createUri("missing.txt"), getCredential());
        assertThrows(FileNotFoundException.class, () -> {
            ResourceUtils.getInputStreamAsBytes(resource.getInputStream());
        });
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
            ResourceUtils.getInputStreamAsBytes(resource.getInputStream());
        });
        createFile("file.txt");
        Resource resource2 = SftpResource.create(createUri("file.txt"), getCredential());
        assertTrue(resource2.lastModified() > 1642968529L);
    }

    @Test
    void length() throws IOException {
        Resource resource = SftpResource.create(createUri("missing.txt"), getCredential());
        assertThrows(FileNotFoundException.class, () -> {
            ResourceUtils.getInputStreamAsBytes(resource.getInputStream());
        });
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

    private void createFile(String fileName) throws IOException {
        try (StatefulResource resource = SftpResource.create(createUri(fileName), getCredential())) {
            ResourceUtils.appendStream(resource.getWriter(), new StringReader("test"));
        }
    }

    private URI createUri(String path) {
        path = ResourceUtils.removeStartSlash(path);
        return URI.create("sftp://localhost:" + sshServer.getPort() + "/" + path);
    }

    private Credential getCredential() {
        return UserPasswordCredential.create(userName, password);
    }

    private void createFiles() throws IOException {
        StatefulResource root = SftpResource.directory(createUri("list"), getCredential());
        root.create();
        root.resolve("dir1", Resource.Type.DIRECTORY).create();
        root.resolve("dir2", Resource.Type.DIRECTORY).create();
        root.resolve("file1.txt", Resource.Type.FILE).create();
        root.resolve("file2.txt", Resource.Type.FILE).create();
    }

    private static SshServer setupServer() {
        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(20000);
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
        if (ResourceUtils.isEmpty(userDir)) {
            userDir = "/tmp";
        }
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