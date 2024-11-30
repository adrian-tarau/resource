package net.microfalx.resource.archive;

import net.microfalx.resource.Resource;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.IOUtils.appendStream;
import static net.microfalx.lang.IOUtils.closeQuietly;

/**
 * Various archive utilities.
 */
public class ArchiveUtils {

    /**
     * Returns whether the resource represent a compressed file.
     *
     * @param resource the resource
     * @return {@code true} if compressed file, {@code false} otherwise
     * @throws IOException if an I/O exception is raised
     */
    public static boolean isCompressed(Resource resource) throws IOException {
        requireNonNull(resource);
        try {
            return CompressorStreamFactory.detect(resource.getInputStream()) != null;
        } catch (CompressorException e) {
            return false;
        }
    }

    /**
     * Returns whether the resource represent a compressed directory (archive).
     *
     * @param resource the resource
     * @return {@code true} if compressed file, {@code false} otherwise
     * @throws IOException if an I/O exception is raised
     */
    public static boolean isArchived(Resource resource) throws IOException {
        requireNonNull(resource);
        try {
            return ArchiveStreamFactory.detect(resource.getInputStream()) != null;
        } catch (ArchiveException e) {
            return false;
        }
    }

    /**
     * Archives a directory or compresses a file.
     *
     * @param source the source file or directory
     * @param target the target archive
     * @throws IOException if an I/O error occurs
     */
    public static void archive(Resource source, Resource target) throws IOException {
        requireNonNull(source);
        requireNonNull(target);
        OutputStream outputStream = target.getOutputStream();
        try {
            if (source.isFile()) {
                archiveFile(source, outputStream);
            } else {
                archiveDirectory(source, outputStream);
            }
            outputStream.close();
        } catch (Exception e) {
            closeQuietly(outputStream);
            try {
                if (target.exists()) target.delete();
            } catch (IOException ex) {
                // ignore if we cannot remove the file
            }
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new IOException(e.getMessage(), e);
            }
        }
    }

    private static void archiveFile(Resource source, OutputStream outputStream) throws IOException, ArchiveException, CompressorException {
        CompressorOutputStream<?> cos = new CompressorStreamFactory().createCompressorOutputStream("gzip", outputStream);
        appendStream(cos, source.getInputStream(), false);
        cos.close();
    }

    private static void archiveDirectory(Resource source, OutputStream outputStream) throws IOException, ArchiveException {
        ArchiveOutputStream<ZipArchiveEntry> aos = new ArchiveStreamFactory().createArchiveOutputStream("zip", outputStream);
        source.walk((root, child) -> {
            if (!child.isFile()) return true;
            String path = child.getPath(root);
            ZipArchiveEntry entry = new ZipArchiveEntry(path);
            entry.setSize(child.length());
            entry.setLastModifiedTime(FileTime.fromMillis(child.lastModified()));
            aos.putArchiveEntry(entry);
            appendStream(aos, child.getInputStream(), false);
            aos.closeArchiveEntry();
            return true;
        });
        aos.close();
    }
}
