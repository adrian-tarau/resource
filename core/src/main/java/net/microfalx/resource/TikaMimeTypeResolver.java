package net.microfalx.resource;

import org.apache.tika.Tika;

import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link MimeTypeResolver} backed by Apache Tika project.
 */
public class TikaMimeTypeResolver implements MimeTypeResolver {

    @Override
    public String detect(InputStream inputStream, String fileName) {
        Tika tika = new Tika();
        try {
            return tika.detect(inputStream);
        } catch (IOException e) {
            // if we cannot detect, just let other resolver to find one
            return null;
        }
    }
}
