package net.microfalx.resource;

import java.io.*;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * Various utilities around resources.
 */
public class ResourceUtils {

    private static final Logger LOGGER = Logger.getLogger(ResourceUtils.class.getName());

    public static final String CLASS_PATH_SCHEME = "classpath";
    public static final String FILE_SCHEME = "file";
    public static final String HTTP_SCHEME = "http";
    public static final String HTTPS_SCHEME = "https";

    public static final String EMPTY_STRING = "";
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static final int BUFFER_SIZE = 128 * 1024;

    /**
     * Holds all metrics related to resource
     */
    protected static Metrics METRICS = Metrics.of("resource");

    /**
     * Checks that the specified object reference is not {@code null}.
     *
     * @param value the object reference to check for nullity
     * @param <T>   the type of the reference
     * @return {@code obj} if not {@code null}
     * @throws NullPointerException if {@code obj} is {@code null}
     */
    public static <T> T requireNonNull(T value) {
        if (value == null) throw new IllegalArgumentException("Argument cannot be NULL");
        return value;
    }

    /**
     * Returns whether the string is empty.
     *
     * @param value the string to validate
     * @return {@code true} if empty, @{code false} otherwise
     */
    public static boolean isEmpty(CharSequence value) {
        return value == null || value.length() == 0;
    }

    /**
     * Returns if the object is "empty": a null object, an empty string({@link CharSequence}) or an empty collection.
     * Any other object type returns false(object not "empty")
     *
     * @param object an object instance
     * @return true if object is considered "empty"(does not carry out information)
     */
    public static boolean isEmpty(Object object) {
        if (object == null) {
            return true;
        } else if (object instanceof CharSequence) {
            return isEmpty((CharSequence) object);
        } else if (object instanceof Collection) {
            return ((Collection<?>) object).isEmpty();
        } else if (object instanceof Map) {
            return ((Map<?, ?>) object).isEmpty();
        } else if (object.getClass().isArray()) {
            return Array.getLength(object) == 0;
        } else {
            return false;
        }
    }

    /**
     * Returns whether the string is not empty.
     *
     * @param value the string to validate
     * @return {@code true} if not empty, @{code false} otherwise
     */
    public static boolean isNotEmpty(CharSequence value) {
        return !isEmpty(value);
    }

    public static String defaultIfEmpty(String value, String defaultValue) {
        return isEmpty(value) ? defaultValue : value;
    }

    public static String defaultIfNull(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static boolean isFileUrl(URL url) {
        return url.getProtocol() == null || "file".equalsIgnoreCase(url.getProtocol());
    }

    public static String[] split(String string, String delims) {
        if (string == null) return EMPTY_STRING_ARRAY;
        StringTokenizer st = new StringTokenizer(string, delims, false);
        String[] values = new String[st.countTokens()];
        int index = 0;
        while (st.hasMoreElements()) {
            String value = (String) st.nextElement();
            values[index++] = value;
        }

        return values;
    }

    /**
     * Removes backslash or forward slash from the value if found at the beginning of the string.
     *
     * @param value the string value
     * @return the string value without backslash or forward slash
     */
    public static String removeStartSlash(String value) {
        if (isEmpty(value)) return value;
        char c = value.charAt(0);
        if (c == '/' || c == '\\') return value.substring(1);
        return value;
    }

    /**
     * Removes backslash or forward slash from the value if found at the end of the string.
     *
     * @param value the string value
     * @return the string value without backslash or forward slash
     */
    public static String removeEndSlash(String value) {
        if (isEmpty(value)) return value;
        char c = value.charAt(value.length() - 1);
        if (c == '/' || c == '\\') return value.substring(0, value.length() - 1);
        return value;
    }

    /**
     * Adds forward slash at the end of the value, if needed.
     *
     * @param value the string value
     * @return the string value without backslash or forward slash
     */
    public static String addEndSlash(String value) {
        if (isEmpty(value)) return "/";
        char c = value.charAt(value.length() - 1);
        if (c != '/') value += "/";
        return value;
    }

    /**
     * Adds forward slash at the beginning of the value, if needed.
     *
     * @param value the string value
     * @return the string value without backslash or forward slash
     */
    public static String addStartSlash(String value) {
        if (isEmpty(value)) return "/";
        char c = value.charAt(0);
        if (c != '/') value = "/" + value;
        return value;
    }

    /**
     * Returns the file extension out of the file name.
     *
     * @param fileName the file name
     * @return the file extension, null if the file has no extension
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null) return null;
        int index = fileName.lastIndexOf('.');
        if (index == -1) return null;
        return fileName.substring(index + 1);
    }

    /**
     * Returns the file name out of a path.
     *
     * @param path the path
     * @return the file name
     */
    public static String getFileName(String path) {
        if (isEmpty(path)) return path;

        int index = path.lastIndexOf('/');
        if (index == -1) index = path.lastIndexOf('\\');
        if (index == -1) return path;
        return path.substring(index + 1);
    }

    /**
     * Returns the parent for a given path.
     *
     * @param path the path
     * @return the parent
     */
    public static String getParentPath(String path) {
        if (isEmpty(path)) return path;

        int index = path.lastIndexOf('/');
        if (index == -1) index = path.lastIndexOf('\\');
        if (index == -1) return null;
        return path.substring(0, index);
    }

    public static String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (value != null) md.update(value.getBytes());
            byte[] data = md.digest();
            return longToId(data, 0) + longToId(data, 8);
        } catch (NoSuchAlgorithmException e) {
            return throwException(e);
        }
    }

    public static String longToId(byte[] data, int offset) {
        long value = data[offset++] + (long) data[offset++] << 8 + (long) data[offset++] << 16 + (long) data[offset++] << 24 + (long) data[offset++] << 32 + (long) data[offset++] << 40 + (long) data[offset++] << 48 + (long) data[offset++] << 56;
        return Long.toString(value, 26);
    }

    /**
     * Converts a String to an identifier.
     * <p>
     * The identifier contains only chars, digits and "_" (or "-" if allowed).
     *
     * @param value the path
     * @return the id
     */
    public static String toIdentifier(String value, boolean allowDash) {
        requireNonNull(value);

        StringBuilder builder = new StringBuilder(value.length());
        char prevChar = 0x00;
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            if (Character.isDigit(c) || Character.isAlphabetic(c) || (allowDash && c == '-')) {
                builder.append(c);
            } else {
                c = '_';
                if (prevChar != c) {
                    builder.append(c);
                }
            }
            prevChar = c;
        }

        String identifier = builder.toString();
        if (identifier.startsWith("_")) {
            identifier = identifier.substring(1);
        }
        if (identifier.endsWith("_")) {
            identifier = identifier.substring(0, identifier.length() - 1);
        }
        return identifier.toLowerCase();
    }

    /**
     * Rethrow a checked exception
     *
     * @param exception an exception
     */
    @SuppressWarnings("SameReturnValue")
    public static <T> T throwException(Throwable exception) {
        doThrowException(exception);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void doThrowException(Throwable exception) throws E {
        requireNonNull(exception);
        throw (E) exception;
    }

    public static void closeQuietly(InputStream is) {
        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to close input stream (type " + is.getClass().getName() + "), reason: " + e.getMessage());
        }
    }

    public static void closeQuietly(OutputStream out) {
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to close output stream (type " + out.getClass().getName() + "), reason: " + e.getMessage());
        }
    }

    public static void closeQuietly(Closeable out) {
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to close stream, reason: " + e.getMessage());
        }
    }

    public static void closeQuietly(AutoCloseable out) {
        try {
            if (out != null) {
                out.close();
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to close stream, reason: " + e.getMessage());
        }
    }

    /**
     * Reads the content of the stream as a String.
     * <p>
     * It expects the String in UTF-8 encoding.
     *
     * @param inputStream the input stream
     * @return the content of the stream as String
     * @throws IOException if an I/O error occurs
     */
    public static String getInputStreamAsString(InputStream inputStream) throws IOException {
        byte[] bytes = getInputStreamAsBytes(inputStream);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Reads the content of the stream into a byte buffer and closes the stream.
     *
     * @param inputStream the input stream
     * @return the content of the stream as byte[]
     * @throws IOException if an I/O error occurs
     */
    public static byte[] getInputStreamAsBytes(InputStream inputStream) throws IOException {
        requireNonNull(inputStream);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        appendStream(out, inputStream);
        return out.toByteArray();
    }

    /**
     * Creates a buffered output stream out of a given file.
     *
     * @param file the file
     * @return a buffered stream, the same object if the stream is already buffered or it does not require any buffer
     */
    public static OutputStream getBufferedOutputStream(File file) throws IOException {
        return getBufferedOutputStream(new FileOutputStream(file));
    }

    /**
     * Creates a buffered output stream out of a given input stream if required.
     *
     * @param outputStream the output stream
     * @return a buffered stream, the same object if the stream is already buffered or it does not require any buffer
     */
    public static OutputStream getBufferedOutputStream(OutputStream outputStream) {
        requireNonNull(outputStream);
        if (outputStream instanceof BufferedOutputStream) {
            return outputStream;
        } else if (outputStream instanceof ByteArrayOutputStream) {
            return outputStream;
        } else {
            return new BufferedOutputStream(outputStream, BUFFER_SIZE);
        }
    }

    /**
     * Creates a buffered writer out of a given writer if required.
     *
     * @param writer the writer
     * @return a buffered writer, the same object if the writer is already buffered or it does not require any buffer
     */
    public static Writer getBufferedWriter(Writer writer) {
        requireNonNull(writer);
        if (writer instanceof BufferedWriter) {
            return writer;
        } else {
            return new BufferedWriter(writer, BUFFER_SIZE);
        }
    }

    /**
     * Creates a buffered input stream out of a given file.
     *
     * @param file the file
     * @return a buffered stream, the same object if the stream is already buffered or it does not require any buffer
     */
    public static InputStream getBufferedInputStream(File file) throws IOException {
        return getBufferedInputStream(new FileInputStream(file));
    }

    /**
     * Creates a buffered input stream out of a given input stream if required.
     *
     * @param inputStream the input stream
     * @return a buffered stream, the same object if the stream is already buffered or it does not require any buffer
     */
    public static InputStream getBufferedInputStream(InputStream inputStream) {
        requireNonNull(inputStream);
        if (inputStream instanceof BufferedInputStream) {
            return inputStream;
        } else if (inputStream instanceof ByteArrayInputStream) {
            return inputStream;
        } else {
            return new BufferedInputStream(inputStream, BUFFER_SIZE);
        }
    }

    /**
     * Creates a buffered reader out of a given reader if required.
     *
     * @param reader the reader
     * @return a buffered stream, the same object if the stream is already buffered or it does not require any buffer
     */
    public static Reader getBufferedReader(Reader reader) {
        requireNonNull(reader);
        if (reader instanceof BufferedReader) {
            return reader;
        } else {
            return new BufferedReader(reader, BUFFER_SIZE);
        }
    }

    /**
     * Copies the input stream content into the output stream. Streams
     * are automatically buffered if they do not implement BufferedInputStream/BufferedOutputStream.
     * <p>
     * The output stream is closed at the end.
     *
     * @param out destination stream
     * @param in  source stream
     * @return number of byte copied from source to destination
     * @throws IOException
     */
    public static long appendStream(OutputStream out, InputStream in) throws IOException {
        return appendStream(out, in, true);
    }

    /**
     * Copies the input stream content into the output stream. Streams
     * are automatically buffered if they do not implement BufferedInputStream/BufferedOutputStream.
     *
     * @param out     destination stream
     * @param in      source stream
     * @param release if true closes the output stream after completion
     * @return number of byte copied from source to destination
     * @throws IOException
     */
    public static long appendStream(OutputStream out, InputStream in, boolean release) throws IOException {
        requireNonNull(out);
        requireNonNull(in);
        // makes sure streams are buffered
        out = getBufferedOutputStream(out);
        in = getBufferedInputStream(in);
        byte[] buffer = new byte[BUFFER_SIZE];
        long totalCopied = 0;
        int count;
        boolean successful = false;
        try {
            while ((count = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
                out.write(buffer, 0, count);
                totalCopied += count;
            }
            out.flush();
            if (release) {
                out.close();
            }
            successful = true;
        } finally {
            closeQuietly(in);
            if (!successful) {
                // if not successful(flush or close did not complete, close the output stream even if release = false to it will not remain open
                closeQuietly(out);
            }
        }
        return totalCopied;
    }

    /**
     * Copies the reader content into the writer. Streams
     * are automatically buffered if they do not implement BufferedInputStream/BufferedOutputStream.
     * <p>
     * The writer is closed at the end.
     *
     * @param out destination stream
     * @param in  source stream
     * @return number of byte copied from source to destination
     * @throws IOException
     */
    public static long appendStream(Writer out, Reader in) throws IOException {
        return appendStream(out, in, true);
    }

    /**
     * Copies the reader content into the writer. Streams
     * are automatically buffered if they do not implement BufferedInputStream/BufferedOutputStream.
     *
     * @param out     destination stream
     * @param in      source stream
     * @param release if true closes the output stream after completion
     * @return number of byte copied from source to destination
     * @throws IOException
     */
    public static long appendStream(Writer out, Reader in, boolean release) throws IOException {
        requireNonNull(out);
        requireNonNull(in);
        // makes sure streams are buffered
        out = getBufferedWriter(out);
        in = getBufferedReader(in);
        char[] buffer = new char[BUFFER_SIZE];
        long totalCopied = 0;
        int count;
        boolean successful = false;
        try {
            while ((count = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
                out.write(buffer, 0, count);
                totalCopied += count;
            }
            out.flush();
            if (release) {
                out.close();
            }
            successful = true;
        } finally {
            closeQuietly(in);
            if (!successful) {
                // if not successful(flush or close did not complete, close the output stream even if release = false to it will not remain open
                closeQuietly(out);
            }
        }
        return totalCopied;
    }
}
