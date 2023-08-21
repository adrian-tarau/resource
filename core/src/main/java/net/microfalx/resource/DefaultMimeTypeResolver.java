package net.microfalx.resource;

import net.microfalx.lang.annotation.Order;

import java.io.InputStream;
import java.net.URLConnection;

@Order(Order.AFTER)
public class DefaultMimeTypeResolver implements MimeTypeResolver {

    @Override
    public String detect(InputStream inputStream, String fileName) {
        return URLConnection.guessContentTypeFromName(fileName);
    }
}
