package net.microfalx.resource;

import net.microfalx.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * A collection of most common mime types, as an enum.
 * <p>
 * Use {@link #getValue()} or {@link #toString()} to retrieve the mime type.
 */
public enum MimeType {

    TEXT_PLAIN("text/plain", true),
    TEXT_CSS("text/css", true),
    TEXT_JAVASCRIPT("text/javascript", true),
    TEXT_HTML("text/html", true),
    TEXT_XML("text/xml", true),
    TEXT_CSV("text/csv", true),
    TEXT("text/*", true),

    IMAGE_PNG("image/png", false),
    IMAGE_BMP("image/bmp", false),
    IMAGE_JPEG("image/jpeg", false),
    IMAGE_GIF("image/gif", false),
    IMAGE_TIFF("image/tiff", false),
    IMAGE("image/*", false),

    FONT("font/*", false),

    APPLICATION_JSON("application/json", true),
    APPLICATION_SQL("application/sql", true),
    APPLICATION_OCTET_STREAM("application/octet-stream", false);

    private final boolean text;
    private final String value;

    /**
     * Returns a mime type enum based on its string value.
     * <p>
     * If it cannot be resolved, it will return {@link #APPLICATION_OCTET_STREAM}.
     * <p>
     * The method also accepts a content type (has an optional character encoding).
     *
     * @param value the mime type (content type)
     * @return a non-null instance
     */
    public static MimeType get(String value) {
        if (StringUtils.isEmpty(value)) return APPLICATION_OCTET_STREAM;
        String[] parts = StringUtils.split(value, ";");
        MimeType mimeType = cache.get(parts[0].toLowerCase());
        return mimeType != null ? mimeType : APPLICATION_OCTET_STREAM;
    }

    MimeType(String value, boolean text) {
        this.value = value;
        this.text = text;
    }

    /**
     * Returns the mime type value.
     *
     * @return a non-null instance
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns whether the given mime type is equal to the one represented by this enum.
     * <p>
     * The equal compares the actual values case-insensitive.
     *
     * @param mimeType the mime type as string
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(String mimeType) {
        return value.equalsIgnoreCase(mimeType);
    }

    /**
     * Returns whether the given mime type is equal to the one represented by this enum.
     * <p>
     * The equal compares the actual values case-insensitive.
     *
     * @param mimeType the mime type as enum
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(MimeType mimeType) {
        if (mimeType == null) return false;
        return value.equalsIgnoreCase(mimeType.value);
    }

    /**
     * Returns whether the mime type indicates a subset of "text/*" or other text based contents.
     *
     * @return {@code true} if a text, {@code false} otherwise
     */
    public boolean isText() {
        return text;
    }

    @Override
    public String toString() {
        return value;
    }

    private static final Map<String, MimeType> cache = new HashMap<>();

    static {
        for (MimeType mimeType : MimeType.values()) {
            cache.put(mimeType.name().toLowerCase(), mimeType);
            cache.put(mimeType.getValue().toLowerCase(), mimeType);
        }
    }
}
