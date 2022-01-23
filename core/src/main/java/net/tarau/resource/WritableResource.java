package net.tarau.resource;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An interface for resources which support writing.
 */
public interface WritableResource extends Resource {

    /**
     * Returns whether the resource can be written.
     * <p>
     * Some resources can read-only even if the resources could be changed.
     *
     * @see #getOutputStream()
     * @see #isReadable()
     */
    default boolean isWritable() {
        return true;
    }

    /**
     * Return an {@link OutputStream} for the underlying resource,
     * allowing to (over-)write its content.
     *
     * @throws IOException if the stream could not be opened
     * @see #getInputStream()
     */
    OutputStream getOutputStream() throws IOException;
}
