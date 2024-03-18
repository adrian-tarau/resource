package net.microfalx.resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * An interface for a mime type resolve
 */
public interface MimeTypeResolver {

    /**
     * Returns the mime type based on resource content and/or file name
     *
     * @param inputStream the input stream of the resource
     * @param fileName    the file name
     * @return the mime type, {@code NULL} if cannot provide one
     */
    String detect(InputStream inputStream, String fileName) throws IOException;
}
