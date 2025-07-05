package com.jaywant.demo.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class ImageUploadService {

    // Base directory for storing images
    private static final String UPLOAD_DIR = "src/main/resources/images/";

    // Allowed image types - Support all common image formats for mobile apps
    private static final String[] ALLOWED_EXTENSIONS = {
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".tiff", ".tif",
            ".svg", ".ico", ".heic", ".heif", ".avif", ".jfif"
    };

    // Allowed MIME types for additional validation
    private static final String[] ALLOWED_MIME_TYPES = {
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp",
            "image/webp", "image/tiff", "image/svg+xml", "image/x-icon",
            "image/vnd.microsoft.icon", "image/heic", "image/heif", "image/avif"
    };

    // Maximum file size (200MB) - Support for ultra-high resolution mobile cameras
    // and iPhones
    private static final long MAX_FILE_SIZE = 200 * 1024 * 1024;

    /**
     * Upload image for work from field attendance
     * 
     * @param file  The image file to upload
     * @param empId Employee ID
     * @param date  Attendance date
     * @return The relative path of the uploaded image
     * @throws IOException If upload fails
     */
    public String uploadAttendanceImage(MultipartFile file, int empId, String date) throws IOException {
        // Validate file
        validateFile(file);

        // Create upload directory if it doesn't exist
        createUploadDirectory();

        // Generate unique filename
        String filename = generateFilename(file, empId, date);

        // Create full path
        Path uploadPath = Paths.get(UPLOAD_DIR + filename);

        // Save file
        Files.copy(file.getInputStream(), uploadPath);

        // Return relative path for database storage
        return "images/" + filename;
    }

    /**
     * Validate uploaded file - Enhanced for mobile app support
     * Validates both file extension and MIME type for security
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds maximum limit of 200MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IOException("Invalid filename");
        }

        String extension = getFileExtension(originalFilename);
        boolean isValidExtension = false;
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (allowedExt.equalsIgnoreCase(extension)) {
                isValidExtension = true;
                break;
            }
        }

        if (!isValidExtension) {
            throw new IOException("Invalid file extension. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // Validate MIME type for additional security
        String contentType = file.getContentType();
        if (contentType != null) {
            boolean isValidMimeType = false;
            for (String allowedMime : ALLOWED_MIME_TYPES) {
                if (contentType.toLowerCase().startsWith(allowedMime.toLowerCase())) {
                    isValidMimeType = true;
                    break;
                }
            }

            if (!isValidMimeType) {
                throw new IOException("Invalid file type. Content-Type: " + contentType + " is not supported");
            }
        }
    }

    /**
     * Create upload directory if it doesn't exist
     */
    private void createUploadDirectory() throws IOException {
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                throw new IOException("Failed to create upload directory");
            }
        }
    }

    /**
     * Generate unique filename for the uploaded image
     */
    private String generateFilename(MultipartFile file, int empId, String date) {
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        // Format: attendance_empId_date_timestamp_uuid.extension
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        return String.format("attendance_%d_%s_%s_%s%s",
                empId,
                date.replace("-", ""),
                timestamp,
                uuid,
                extension);
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    /**
     * Get supported image formats as a user-friendly string
     */
    public static String getSupportedFormats() {
        return "Supported formats: JPG, JPEG, PNG, GIF, BMP, WEBP, TIFF, SVG, ICO, HEIC, HEIF, AVIF, JFIF (Max size: 200MB)";
    }

    /**
     * Delete image file
     */
    public boolean deleteImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return false;
        }

        try {
            // Remove "images/" prefix if present
            String filename = imagePath.startsWith("images/") ? imagePath.substring(7) : imagePath;

            Path filePath = Paths.get(UPLOAD_DIR + filename);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Check if file exists
     */
    public boolean imageExists(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return false;
        }

        String filename = imagePath.startsWith("images/") ? imagePath.substring(7) : imagePath;

        Path filePath = Paths.get(UPLOAD_DIR + filename);
        return Files.exists(filePath);
    }
}
