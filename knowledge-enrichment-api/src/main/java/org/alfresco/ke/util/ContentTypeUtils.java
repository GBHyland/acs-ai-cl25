package org.alfresco.ke.util;

import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

/**
 * Utility class for determining the MIMETYPE of a file using Apache Tika
 */
public final class ContentTypeUtils {

    private static final Tika tika = new Tika();

    private ContentTypeUtils() {}

    /**
     * Determines the MIME type of a multipart file using content sniffing,
     * with fallback to the file's declared content type.
     *
     * @param file the uploaded multipart file
     * @return the detected MIME type, or "application/octet-stream" if unknown
     */
    public static String determineContentType(MultipartFile file) {
        try {
            // Most accurate: analyze file bytes
            String detected = tika.detect(file.getBytes(), file.getOriginalFilename());
            if (detected != null && !detected.equals("application/octet-stream")) {
                return detected;
            }
        } catch (Exception ignored) {
        }

        // Fallback: use browser-declared type
        String fallback = file.getContentType();
        return fallback != null ? fallback : "application/octet-stream";
    }
}
