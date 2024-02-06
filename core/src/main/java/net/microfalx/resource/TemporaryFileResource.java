package net.microfalx.resource;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.resource.ResourceUtils.hash;

public class TemporaryFileResource extends FileResource {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    /**
     * Create a new file resource in JVM temporary directory.
     *
     * @param prefix the prefix of the temporary file
     * @param suffix the suffix (an extension, optional) of the temporary file
     * @return a non-null instance
     */
    public static Resource file(String prefix, String suffix) {
        requireNonNull(prefix);
        String fileName = prefix + Long.toString(System.currentTimeMillis(), 26) + SEQUENCE.getAndDecrement();
        if (suffix != null) fileName += suffix;
        return file(fileName);
    }

    /**
     * Create a new directory resource in JVM temporary directory.
     *
     * @param prefix the prefix of the temporary file
     * @param suffix the suffix (optional) of the temporary file
     * @return a non-null instance
     */
    public static Resource directory(String prefix, String suffix) {
        requireNonNull(prefix);
        String fileName = prefix + Long.toString(System.currentTimeMillis(), 26) + SEQUENCE.getAndDecrement();
        if (suffix != null) fileName += suffix;
        return directory(fileName);
    }

    /**
     * Create a new file resource in JVM temporary directory.
     *
     * @param fileName the file of the temporary file
     * @return a non-null instance
     */
    public static Resource directory(String fileName) {
        return create(Type.DIRECTORY, fileName);
    }

    /**
     * Create a new file resource in JVM temporary directory.
     *
     * @param fileName the file of the temporary file
     * @return a non-null instance
     */
    public static Resource file(String fileName) {
        return create(Type.FILE, fileName);
    }

    /**
     * Create a new file resource in JVM temporary directory.
     *
     * @param fileName the file of the temporary file
     * @return a non-null instance
     */
    public static Resource create(Type type, String fileName) {
        requireNonNull(type);
        requireNonNull(fileName);
        File file = new File(getTemporaryDirectory(), fileName);
        return new FileResource(type, hash(file.getAbsolutePath()), file);
    }

    protected TemporaryFileResource(Type type, String id, File file) {
        super(type, id, file);
    }

    static File getTemporaryDirectory() {
        return new File(System.getProperty("java.io.tmpdir"));
    }
}
