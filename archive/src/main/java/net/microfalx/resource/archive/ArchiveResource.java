package net.microfalx.resource.archive;

import net.microfalx.lang.FileUtils;
import net.microfalx.metrics.Metrics;
import net.microfalx.resource.*;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ResolutionException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.ExceptionUtils.throwException;
import static net.microfalx.lang.IOUtils.getUnclosableInputStream;
import static net.microfalx.lang.StringUtils.removeEndSlash;
import static net.microfalx.lang.StringUtils.removeStartSlash;
import static net.microfalx.lang.UriUtils.removeFragment;
import static net.microfalx.resource.ResourceUtils.throwUnsupported;

/**
 * A resource implementation on top of Apache Common Compress.
 */
public final class ArchiveResource extends AbstractResource {

    private static final Metrics METRICS = ResourceUtils.METRICS.withGroup("File");

    private final Resource resource;
    private Type archiveType;

    private final static Map<String, Type> extensionToType = new LinkedHashMap<>();
    private final static Map<String, Type> libTypeToType = new LinkedHashMap<>();
    private final static Map<Type, String> typeToLibType = new LinkedHashMap<>();

    /**
     * Create a new archive resource from another resource.
     *
     * @param uri the URI of the resource
     * @return a non-null instance
     */
    public static Resource create(URI uri) {
        Resource resource = ResourceFactory.resolve(uri);
        return create(resource, Type.AUTO);
    }

    /**
     * Create a new archive resource from another resource.
     *
     * @param resource the resource
     * @return a non-null instance
     */
    public static Resource create(Resource resource) {
        return create(resource, Type.AUTO);
    }

    /**
     * Returns an archive type form the resource file name extension.
     * <p>
     * If a type cannot be determined, {@link Type#AUTO} will be returned and leave the auto-detection at stream level.
     *
     * @param uri the resource URI
     * @return a non-null instance
     */
    public static Type fromExtension(URI uri) {
        requireNonNull(uri);
        return fromFileName(uri.getPath());
    }

    /**
     * Returns an archive type form the resource file name extension.
     * <p>
     * If a type cannot be determined, {@link Type#AUTO} will be returned and leave the auto-detection at stream level.
     *
     * @param resource the resource
     * @return a non-null instance
     */
    public static Type fromExtension(Resource resource) {
        requireNonNull(resource);
        return fromFileName(resource.getFileName());
    }

    /**
     * Create a new archive resource from another resource.
     *
     * @param resource the URI of the resource
     * @return a non-null instance
     */
    public static Resource create(Resource resource, Type type) {
        requireNonNull(resource);
        requireNonNull(type);
        if (resource.isDirectory()) throw new ResolutionException("An archive file is required to create a resource");
        return new ArchiveResource(resource, type);
    }

    private ArchiveResource(Resource resource, Type type) {
        super(type.getType(), resource.getId());
        this.resource = resource;
        this.archiveType = type;
        setFragment(resource.getFragment());
    }

    /**
     * Returns the archive type.
     *
     * @return a non-null instance
     */
    public Type getArchiveType() {
        if (archiveType == Type.AUTO) archiveType = detect();
        return archiveType;
    }

    @Override
    public String getFileName() {
        if (hasFragment()) {
            return FileUtils.getFileName(getFragment());
        } else {
            return resource.getFileName();
        }
    }

    @Override
    public Resource resolve(String path, Resource.Type type) {
        return get(path, type);
    }

    @Override
    public Resource get(String path, Resource.Type type) {
        checkType(type);
        if (hasFragment()) {
            throw new ResourceException("Cannot resolve an archive entry (" + path + ") from an archive entry (" + toURI() + ")");
        }
        return ArchiveResource.create(resource).withFragment(path);
    }

    @Override
    protected Resource.Type calculateType(Resource.Type type) throws IOException {
        return getArchiveType().isContainer() ? Resource.Type.DIRECTORY : Resource.Type.FILE;
    }

    @Override
    protected InputStream doGetInputStream(boolean raw) throws IOException {
        if (!resource.exists()) {
            throw new FileNotFoundException("Archive '" + toURINoFragment() + "' does not exist");
        }
        if (hasFragment()) {
            return localEntryInputStream(getFragment());
        } else {
            Type archiveType = getArchiveType();
            if (!archiveType.isContainer() && !raw) {
                try {
                    CompressorInputStream compressorInputStream = new CompressorStreamFactory().createCompressorInputStream(resource.getInputStream(true));
                    return new BufferedInputStream(compressorInputStream, BUFFER_SIZE);
                } catch (CompressorException e) {
                    throw new ResourceException(e.getMessage(), e);
                }
            } else {
                return resource.getInputStream();
            }
        }
    }

    @Override
    public URI toURI() {
        return resource.toURI();
    }

    @Override
    protected boolean doWalk(ResourceVisitor visitor, int maxDepth) throws IOException {
        Type archiveType = getArchiveType();
        if (archiveType.isContainer()) {
            ArchiveInputStream stream = openArchiveStream();
            boolean completed = true;
            for (; ; ) {
                ArchiveEntry entry = stream.getNextEntry();
                if (entry == null) break;
                int depth = ResourceUtils.getDepth(entry.getName());
                if (depth <= maxDepth) {
                    ArchiveEntryResource entryResource = new ArchiveEntryResource(this.resource.getId() + ":" + entry.getName(), stream, entry);
                    boolean shouldContinue = visitor.onResource(this, entryResource);
                    if (!shouldContinue) {
                        completed = false;
                        break;
                    }
                }
            }
            return completed;
        } else {
            return false;
        }
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    protected Metrics getMetrics() {
        return METRICS;
    }

    private InputStream localEntryInputStream(String path) throws IOException {
        Type archiveType = getArchiveType();
        if (!archiveType.isContainer()) {
            throw new ResourceException("Archive '" + toURINoFragment() + "' is not a container");
        }
        path = removeStartSlash(path);
        if (archiveType.isRandomAccess()) {
            return localEntryInputStreamRandomAccess(path);
        } else {
            return localEntryInputStreamSequenceAccess(path);
        }
    }

    private InputStream localEntryInputStreamRandomAccess(String path) throws IOException {
        Type archiveType = getArchiveType();
        if (archiveType.equals(Type.ZIP)) {
            FileResource fileResource = (FileResource) resource.toFile();
            ZipFile zipFile = new ZipFile(fileResource.getFile());
            ZipEntry entry = zipFile.getEntry(path);
            if (entry != null) {
                return zipFile.getInputStream(entry);
            } else {
                return throwEntryNotFound(path);
            }
        } else {
            throw new ResourceException("Unsupported random access to archive '" + toURINoFragment() + "'");
        }
    }

    private InputStream localEntryInputStreamSequenceAccess(String path) throws IOException {
        InputStream inputStream = null;
        ArchiveInputStream stream = openArchiveStream();
        for (; ; ) {
            ArchiveEntry entry = stream.getNextEntry();
            if (entry == null) break;
            if (!entry.isDirectory() && path.equalsIgnoreCase(removeStartSlash(entry.getName()))) {
                inputStream = stream;
                break;
            }
        }
        if (inputStream == null) throwEntryNotFound(path);
        return inputStream;
    }

    private <T> T throwEntryNotFound(String path) {
        throw new ResourceException("An archive entry '" + path + "' does not exists in archive '" + removeFragment(toURI()) + "'");
    }

    private ArchiveInputStream openArchiveStream() throws IOException {
        try {
            return new ArchiveStreamFactory().createArchiveInputStream(resource.getInputStream(true));
        } catch (ArchiveException e) {
            throw new ResourceException("Failed to open archive '" + resource.toURI() + "'", e);
        }
    }

    private Type detect() {
        String type;
        try {
            InputStream inputStream = resource.getInputStream(true);
            inputStream.mark(100);
            try {
                type = CompressorStreamFactory.detect(inputStream);
            } catch (CompressorException e) {
                inputStream.reset();
                try {
                    type = ArchiveStreamFactory.detect(inputStream);
                } finally {
                    inputStream.reset();
                }
            }
        } catch (Exception e) {
            return throwException(e);
        }
        Type discoveredType = libTypeToType.get(type);
        if (discoveredType == null) throw new ResolutionException("Unknown archive type for " + toURI());
        return discoveredType;
    }

    private void checkType(Resource.Type type) {
        if (type == Resource.Type.DIRECTORY)
            throw new ResolutionException("An archive file is required to create a resource");
    }

    private static ArchiveResource.Type fromFileName(String fileName) {
        if (fileName == null) return Type.AUTO;
        fileName = fileName.toLowerCase();
        for (Map.Entry<String, Type> entry : extensionToType.entrySet()) {
            if (fileName.endsWith(entry.getKey())) return entry.getValue();
        }
        return Type.AUTO;
    }

    private class ArchiveEntryResource extends AbstractResource {

        private final ArchiveInputStream stream;
        private final ArchiveEntry entry;
        private AtomicBoolean read = new AtomicBoolean();

        public ArchiveEntryResource(String id, ArchiveInputStream stream, ArchiveEntry entry) {
            super(entry.isDirectory() ? Type.DIRECTORY : Type.FILE, id);

            this.stream = stream;
            this.entry = entry;
        }

        @Override
        public String getFileName() {
            return removeEndSlash(entry.getName());
        }

        @Override
        protected long doLength() throws IOException {
            return entry.getSize();
        }

        @Override
        protected long doLastModified() throws IOException {
            return entry.getLastModifiedDate().getTime();
        }

        @Override
        public Resource resolve(String path, Type type) {
            return throwUnsupported();
        }

        @Override
        public Resource get(String path, Type type) {
            return throwUnsupported();
        }

        @Override
        public InputStream doGetInputStream(boolean raw) throws IOException {
            if (read.compareAndSet(false, true)) throw new ResourceException("The stream for entry '"
                    + entry.getName() + "' was already processed");
            return getUnclosableInputStream(stream);
        }

        @Override
        public URI toURI() {
            return null;
        }
    }

    public enum Type {

        AUTO(false, false),
        BROTLI(false, false),
        BZIP2(false, false),
        GZIP(false, false),
        PACK200(false, false),
        XZ(false, false),
        LZMA(false, false),
        SNAPPY_FRAMED(false, false),
        SNAPPY_RAW(false, false),
        DEFLATE(false, false),
        DEFLATE64(false, false),
        LZ4_BLOCK(false, false),
        LZ4_FRAMED(false, false),
        Z(false, false),
        ZSTANDARD(false, false),

        ZIP(true, true),
        JAR(true, true),
        TAR(true, false),
        SEVEN_Z(true, false);

        private boolean container;
        private boolean randomAccess;

        Type(boolean container, boolean randomAccess) {
            this.container = container;
            this.randomAccess = randomAccess;
        }

        public boolean isContainer() {
            return container;
        }

        public boolean isRandomAccess() {
            return randomAccess;
        }

        public Resource.Type getType() {
            return container ? Resource.Type.DIRECTORY : Resource.Type.FILE;
        }
    }

    static {
        extensionToType.put("br", Type.BROTLI);
        extensionToType.put("bzip2", Type.BZIP2);
        extensionToType.put("bz2", Type.BZIP2);
        extensionToType.put("gzip", Type.GZIP);
        extensionToType.put("gz", Type.GZIP);
        extensionToType.put("xz", Type.XZ);
        extensionToType.put("lz", Type.LZ4_BLOCK);
        extensionToType.put("7z", Type.SEVEN_Z);
        extensionToType.put("z", Type.Z);
        extensionToType.put("zip", Type.ZIP);
        extensionToType.put("jar", Type.JAR);
        extensionToType.put("tar", Type.JAR);

        typeToLibType.put(Type.BROTLI, CompressorStreamFactory.BROTLI);
        typeToLibType.put(Type.BZIP2, CompressorStreamFactory.BZIP2);
        typeToLibType.put(Type.GZIP, CompressorStreamFactory.GZIP);
        typeToLibType.put(Type.PACK200, CompressorStreamFactory.PACK200);
        typeToLibType.put(Type.XZ, CompressorStreamFactory.XZ);
        typeToLibType.put(Type.LZMA, CompressorStreamFactory.LZMA);
        typeToLibType.put(Type.SNAPPY_FRAMED, CompressorStreamFactory.SNAPPY_FRAMED);
        typeToLibType.put(Type.SNAPPY_RAW, CompressorStreamFactory.SNAPPY_RAW);
        typeToLibType.put(Type.DEFLATE, CompressorStreamFactory.DEFLATE);
        typeToLibType.put(Type.DEFLATE64, CompressorStreamFactory.DEFLATE64);
        typeToLibType.put(Type.LZ4_BLOCK, CompressorStreamFactory.LZ4_BLOCK);
        typeToLibType.put(Type.LZ4_FRAMED, CompressorStreamFactory.LZ4_FRAMED);
        typeToLibType.put(Type.Z, CompressorStreamFactory.Z);
        typeToLibType.put(Type.ZSTANDARD, CompressorStreamFactory.ZSTANDARD);

        typeToLibType.put(Type.ZIP, ArchiveStreamFactory.ZIP);
        typeToLibType.put(Type.JAR, ArchiveStreamFactory.JAR);
        typeToLibType.put(Type.TAR, ArchiveStreamFactory.TAR);
        typeToLibType.put(Type.SEVEN_Z, ArchiveStreamFactory.SEVEN_Z);

        libTypeToType.put(CompressorStreamFactory.BROTLI, Type.BROTLI);
        libTypeToType.put(CompressorStreamFactory.BZIP2, Type.BZIP2);
        libTypeToType.put(CompressorStreamFactory.GZIP, Type.GZIP);
        libTypeToType.put(CompressorStreamFactory.PACK200, Type.PACK200);
        libTypeToType.put(CompressorStreamFactory.XZ, Type.XZ);
        libTypeToType.put(CompressorStreamFactory.LZMA, Type.LZMA);
        libTypeToType.put(CompressorStreamFactory.SNAPPY_FRAMED, Type.SNAPPY_FRAMED);
        libTypeToType.put(CompressorStreamFactory.SNAPPY_RAW, Type.SNAPPY_RAW);
        libTypeToType.put(CompressorStreamFactory.DEFLATE, Type.DEFLATE);
        libTypeToType.put(CompressorStreamFactory.DEFLATE64, Type.DEFLATE64);
        libTypeToType.put(CompressorStreamFactory.LZ4_BLOCK, Type.LZ4_BLOCK);
        libTypeToType.put(CompressorStreamFactory.LZ4_FRAMED, Type.LZ4_FRAMED);
        libTypeToType.put(CompressorStreamFactory.Z, Type.Z);
        libTypeToType.put(CompressorStreamFactory.ZSTANDARD, Type.ZSTANDARD);

        libTypeToType.put(ArchiveStreamFactory.SEVEN_Z, Type.SEVEN_Z);
        libTypeToType.put(ArchiveStreamFactory.ZIP, Type.ZIP);
        libTypeToType.put(ArchiveStreamFactory.JAR, Type.JAR);
        libTypeToType.put(ArchiveStreamFactory.TAR, Type.TAR);
    }
}
