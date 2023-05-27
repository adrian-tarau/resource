package net.microfalx.resource;

import java.io.*;
import java.util.logging.Logger;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * A skeleton implementation for a resource which keeps a state (like a connection to a remote resource).
 */
public abstract class AbstractStatefulResource<S, C> extends AbstractResource implements StatefulResource {

    private static Logger LOGGER = Logger.getLogger(AbstractStatefulResource.class.getName());

    private volatile S session;

    public AbstractStatefulResource(Type type, String id) {
        super(type, id);
    }

    @Override
    public void close() {
        releaseSession();
    }

    /**
     * Creates a session to be used with the resource.
     * <p>
     * The session remains open until {@link  #close()} is called.
     *
     * @return the caches session
     * @throws IOException if an I/O exception occurs
     */
    protected synchronized S createSession() throws IOException {
        if (session != null) {
            try {
                if (isValid(session)) return session;
            } catch (Exception e) {
                LOGGER.warning("Failed to validate session for '" + toURI() + "', close session, root cause " + e.getMessage());
            }
        }
        try {
            session = doCreateSession();
            return session;
        } catch (Exception e) {
            throw new IOException("Failed to create session for '" + toURI() + "'", e);
        }
    }

    /**
     * Releases the current session, if a valid session exists
     */
    protected synchronized void releaseSession() {
        if (session == null) return;
        try {
            doReleaseSession(session);
        } catch (Exception e) {
            LOGGER.warning("Failed to release session for '" + toURI() + "', root cause " + e.getMessage());
        } finally {
            session = null;
        }
    }

    /**
     * Subclasses will actually create the session instance.
     *
     * @return a non-null instance
     * @throws Exception if an error occurs
     */
    protected abstract S doCreateSession() throws Exception;

    /**
     * Sunclasses will actually release a session instance.
     *
     * @param session the session
     * @throws Exception if an error occurs
     */
    protected abstract void doReleaseSession(S session) throws Exception;

    /**
     * Validates whether the session is still valid and can be used.
     * <p>
     * Any exception is handled by closing the session session and open a new one
     *
     * @param session the session
     * @return {@code true} if valid, {@code false} othersie
     * @throws Exception if an error occurs
     */
    protected abstract boolean isValid(S session) throws Exception;

    /**
     * Subclasses will actually create a channel for an existing session.
     *
     * @param session the session
     * @return a non-null instance
     * @throws IOException if an I/O error occurs
     */
    protected C createChannel(S session) throws IOException {
        try {
            return doCreateChannel(session);
        } catch (Exception e) {
            throw new IOException("Failed to create connection for '" + toURI() + "'", e);
        }
    }

    /**
     * Subclasses will actually release the channel.
     *
     * @param session the session
     * @param channel the channel
     * @throws IOException if an I/O error occurs
     */
    protected void releaseChannel(S session, C channel) throws IOException {
        if (channel == null) return;
        try {
            doReleaseChannel(session, channel);
        } catch (Exception e) {
            LOGGER.warning("Failed to release connection for '" + toURI() + "', root cause +" + e.getMessage());
        }
    }

    /**
     * Subclasses will actually create the channel.
     *
     * @param session the session
     * @throws Exception if an error occurs
     */
    protected abstract C doCreateChannel(S session) throws Exception;

    /**
     * Subclasses will actually release the channel.
     *
     * @param session the session
     * @param channel the channel
     * @throws Exception if an  error occurs
     */
    protected abstract void doReleaseChannel(S session, C channel) throws Exception;

    /**
     * Translates a session/channel specific exception into an I/O exception.
     *
     * @param e the original exception
     * @return the translated exception
     */
    protected abstract IOException translateException(Exception e);

    /**
     * Executes a command with a channel.
     *
     * @param command  the command (for logging purposes)
     * @param callback the callback
     * @param <R>      the return type
     * @return the value returned by the callback
     * @throws IOException if an I/O error occurs
     */
    protected final <R> R doWithChannel(String command, ChannelCallback<C, R> callback) throws IOException {
        requireNonNull(callback);
        S session = createSession();
        C channel = null;
        try {
            channel = createChannel(session);
            return callback.doWithChannel(channel);
        } catch (Exception e) {
            throw translateException(e);
        } finally {
            releaseChannel(session, channel);
        }
    }

    /**
     * A callback interface used to perform action on a channel under a try {} finally{} block.
     *
     * @param <C> the channel implementation
     * @param <R> the result type
     */
    @FunctionalInterface
    protected interface ChannelCallback<C, R> {

        R doWithChannel(C channel) throws Exception;

    }

    protected class StatefulInputStream extends FilterInputStream {

        private final S session;
        private final C channel;

        public StatefulInputStream(InputStream inputStream, S session, C channel) {
            super(inputStream);

            this.session = session;
            this.channel = channel;
        }

        @Override
        public void close() throws IOException {
            super.close();

            releaseChannel(session, channel);
        }
    }

    protected class SftpOutputStream extends FilterOutputStream {

        private final S session;
        private final C channel;

        public SftpOutputStream(OutputStream outputStream, S session, C channel) {
            super(outputStream);

            this.session = session;
            this.channel = channel;
        }

        @Override
        public void close() throws IOException {
            super.close();

            releaseChannel(session, channel);
        }
    }
}
