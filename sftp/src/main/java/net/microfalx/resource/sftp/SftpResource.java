package net.microfalx.resource.sftp;

import com.jcraft.jsch.*;
import net.microfalx.lang.FileUtils;
import net.microfalx.metrics.Metrics;
import net.microfalx.resource.*;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;
import java.util.logging.Logger;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.IOUtils.appendStream;
import static net.microfalx.lang.StringUtils.*;
import static net.microfalx.resource.ResourceUtils.getTypeFromPath;

/**
 * A resource for a SFTP resource.
 */
public class SftpResource extends AbstractStatefulResource<Session, ChannelSftp> {

    private static final Logger LOGGER = Logger.getLogger(SftpResource.class.getName());

    private static final Metrics METRICS = ResourceUtils.METRICS.withGroup("S3");

    private final URI uri;

    /**
     * Creates a resource with a file type.
     *
     * @param uri        the URI
     * @param credential the credential
     * @return a non-null instance
     */
    public static StatefulResource file(URI uri, Credential credential) {
        return create(Type.FILE, uri, credential);
    }

    /**
     * Creates a resource with a directory type.
     *
     * @param uri        the URI
     * @param credential the credential
     * @return a non-null instance
     */
    public static StatefulResource directory(URI uri, Credential credential) {
        return create(Type.DIRECTORY, uri, credential);
    }

    /**
     * Creates a resource and auto-detects the type.
     *
     * @param uri        the URI
     * @param credential the credential
     * @return a non-null instance
     */
    public static StatefulResource create(URI uri, Credential credential) {
        requireNonNull(uri);
        String path = uri.getPath();
        Type type = getTypeFromPath(path, null);
        return create(type, uri, credential);
    }

    /**
     * Creates a resource and auto-detects the type.
     *
     * @param type       the resource type
     * @param uri        the URI
     * @param credential the credential
     * @return a non-null instance
     */
    public static StatefulResource create(Type type, URI uri, Credential credential) {
        requireNonNull(type);
        requireNonNull(uri);
        requireNonNull(credential);
        String id = ResourceUtils.hash(uri.toASCIIString());
        SftpResource resource = new SftpResource(type, id, uri);
        resource.setCredential(credential);
        return resource;
    }

    private SftpResource(Type type, String id, URI uri) {
        super(type, id);
        requireNonNull(uri);
        this.uri = uri;
        setAbsolutePath(false);
    }

    @Override
    public String getFileName() {
        return FileUtils.getFileName(uri.getPath());
    }

    @Override
    public InputStream doGetInputStream(boolean raw) throws IOException {
        Session session = createSession();
        ChannelSftp channel = createChannel(session);
        try {
            InputStream inputStream = channel.get(getRealPath());
            return new StatefulInputStream(inputStream, session, channel);
        } catch (SftpException e) {
            throw translateException(e);
        }
    }

    @Override
    public OutputStream doGetOutputStream() throws IOException {
        Session session = createSession();
        ChannelSftp channel = createChannel(session);
        try {
            OutputStream outputStream = channel.put(getRealPath());
            return new SftpOutputStream(outputStream, session, channel);
        } catch (SftpException e) {
            throw translateException(e);
        }
    }

    @Override
    public void doCreate() throws IOException {
        if (exists()) return;
        if (getType() == Type.FILE) {
            appendStream(getWriter(), new StringReader(EMPTY_STRING));
        } else {
            Resource parent = getParent();
            if (parent != null && !parent.exists()) parent.create();
            doWithChannel("create_directory", channel -> {
                channel.mkdir(getRealPath());
                return null;
            });
        }
    }

    @Override
    public boolean doExists() throws IOException {
        try {
            return doWithChannel("exists", channel -> {
                SftpATTRS attrs = channel.lstat(getRealPath());
                return attrs != null;
            });
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    @Override
    protected long doLastModified() throws IOException {
        return doWithChannel("last_modified", channel -> {
            SftpATTRS attrs = channel.lstat(getRealPath());
            return (long) attrs.getMTime() * 1000;
        });
    }

    @Override
    protected long doLength() throws IOException {
        return doWithChannel("last_modified", channel -> {
            SftpATTRS attrs = channel.lstat(getRealPath());
            return attrs.getSize();
        });
    }

    @Override
    protected Collection<Resource> doList() throws IOException {
        return doWithChannel("list", channel -> {
            Vector<Object> entries = channel.ls(getRealPath());
            Collection<Resource> children = new ArrayList<>(entries.size());
            for (Object entry : entries) {

            }
            return children;
        });
    }

    @Override
    public Resource resolve(String path) {
        requireNonNull(path);
        Type type = getTypeFromPath(path);
        return resolve(path, type);
    }

    @Override
    public Resource resolve(String path, Type type) {
        requireNonNull(path);
        requireNonNull(type);
        String newUri = addEndSlash(uri.toASCIIString()) + path;
        return createFromUri(newUri, type);
    }

    @Override
    public Resource get(String path, Type type) {
        requireNonNull(path);
        requireNonNull(type);
        try {
            URI newUri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), path, null, null);
            return createFromUri(newUri.toASCIIString(), type);
        } catch (URISyntaxException e) {
            throw new ResourceException("Invalid resource URL for path '" + path + "', original URI '" + uri + "'", e);
        }
    }

    @Override
    public URI toURI() {
        return uri;
    }

    @Override
    protected Session doCreateSession() throws Exception {
        Credential credential = getCredential();
        JSch jsch = new JSch();
        jsch.setHostKeyRepository(new HostKeyRepositoryImpl());
        Session session;
        if (credential instanceof UserPasswordCredential) {
            UserPasswordCredential upc = (UserPasswordCredential) credential;
            session = jsch.getSession(upc.getUserName(), uri.getHost());
            if (uri.getPort() > 0) session.setPort(uri.getPort());
            session.setPassword(upc.getPassword());
        } else {
            throw new IllegalArgumentException("Unexpected credential type " + credential.getClass().getName());
        }
        session.connect();
        return session;
    }

    @Override
    protected void doReleaseSession(Session session) throws Exception {
        session.disconnect();
    }

    @Override
    protected ChannelSftp doCreateChannel(Session session) throws Exception {
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();
        return channelSftp;
    }

    @Override
    protected void doReleaseChannel(Session session, ChannelSftp channel) throws Exception {
        channel.disconnect();
    }

    @Override
    protected boolean isValid(Session session) throws Exception {
        return session.isConnected();
    }

    @Override
    protected Metrics getMetrics() {
        return METRICS;
    }

    @Override
    protected IOException translateException(Exception e) {
        if (e instanceof SftpException) {
            SftpException sftpException = (SftpException) e;
            switch (sftpException.id) {
                case ChannelSftp.SSH_FX_PERMISSION_DENIED:
                    return new IOException("Permission denied for '" + toURI() + "'");
                case ChannelSftp.SSH_FX_NO_SUCH_FILE:
                    return new FileNotFoundException("File '" + uri.getPath() + "' does not exist at '" + uri + "'");
                default:
                    return new IOException("SFTP action failed for '" + toURI() + "'", e);
            }
        } else if (e instanceof JSchException) {
            return new IOException("SSH action failed for '" + toURI() + "'", e);
        } else {
            return new IOException("Unknown failure for resource '" + toURI() + "'", e);
        }
    }

    private String getRealPath() {
        String path = toURI().getPath();
        if (isAbsolutePath()) {
            return addStartSlash(path);
        } else {
            return removeStartSlash(path);
        }
    }

    private IOException translate(SftpException exception) {
        switch (exception.id) {
            case ChannelSftp.SSH_FX_PERMISSION_DENIED:
                return new IOException("Permission denied for '" + uri.getPath() + "'");
            case ChannelSftp.SSH_FX_NO_SUCH_FILE:
                return new FileNotFoundException("File '" + uri.getPath() + "' does not exist");
            default:
                return new IOException("SFTP action failed for '" + uri.getPath() + "'", exception);
        }
    }

    /**
     * Creates a new resource from a URI and a type, using the same credentials as this resource.
     *
     * @param uri  the URI as string
     * @param type the type
     * @return a new instance
     */
    private Resource createFromUri(String uri, Type type) {
        return SftpResource.create(type, URI.create(uri), getCredential());
    }

    static class HostKeyRepositoryImpl implements HostKeyRepository {

        @Override
        public int check(String host, byte[] key) {
            return OK;
        }

        @Override
        public void add(HostKey hostkey, UserInfo ui) {

        }

        @Override
        public void remove(String host, String type) {

        }

        @Override
        public void remove(String host, String type, byte[] key) {

        }

        @Override
        public String getKnownHostsRepositoryID() {
            return "null";
        }

        @Override
        public HostKey[] getHostKey() {
            return new HostKey[0];
        }

        @Override
        public HostKey[] getHostKey(String host, String type) {
            return new HostKey[0];
        }
    }
}
