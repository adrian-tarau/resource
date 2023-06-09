package net.microfalx.resource.archive;

import net.microfalx.resource.Resource;
import net.microfalx.resource.ResourceProcessor;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.InputStream;

/**
 * A processor which tries to decompress the stream if the stream is compressed.
 */
public class ArchiveResourceProcessor implements ResourceProcessor {

    @Override
    public InputStream getInputStream(Resource resource, InputStream inputStream) {
        try {
            String format = CompressorStreamFactory.detect(inputStream);
            return new CompressorStreamFactory().createCompressorInputStream(format, inputStream);
        } catch (CompressorException e) {
            return inputStream;
        }
    }

    @Override
    public int getOrder() {
        return HIGH_PRIORITY;
    }
}
