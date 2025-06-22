package dev.ruthvik.jsonServer.handler;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UploadedFile;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FileUploadHandler implements Handler {

    private final Logger logger = LoggerFactory.getLogger(FileUploadHandler.class);
    private final long MAX_UPLOAD_SIZE;
    private final Path uploadDir;

    public FileUploadHandler(long maxUploadSize, Path uploadDir) {
        this.MAX_UPLOAD_SIZE = maxUploadSize;
        this.uploadDir = uploadDir;
    }

    @Override
    public void handle(@NotNull Context context) {
        final String errorResponse = "{\"error\":\"%s\"}";

        UploadedFile uploadedFile = context.uploadedFile("image");
        if (uploadedFile == null) {
            context.status(400).json(String.format(errorResponse, "No image provided"));
            return;
        }

        String originalName = uploadedFile.filename();
        if (originalName.trim().isEmpty()) {
            context.status(400).json(String.format(errorResponse, "Invalid file name"));
            return;
        }

        // Check MIME type
        String contentType = uploadedFile.contentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            context.status(400).json(String.format(errorResponse, "Only image uploads are allowed"));
            return;
        }

        // Check file size
        if (uploadedFile.size() > MAX_UPLOAD_SIZE) {
            context.status(413).json(String.format(errorResponse, "File too large"));
            return;
        }

        // ✅ Sanitize filename
        String cleanFileName = originalName.trim()
                .replaceAll("[\\\\/]", "")
                .replaceAll("\\p{Cntrl}", "")
                .replaceAll("\\s+", "_");

        // Block dangerous extensions
        if (cleanFileName.endsWith(".yml") || cleanFileName.endsWith(".yaml")
                || cleanFileName.endsWith(".js") || cleanFileName.endsWith(".html")) {
            context.status(400).json(String.format(errorResponse, "Unsupported file type"));
            return;
        }

        // Ensure upload directory exists
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            logger.error("Failed to create upload directory: {}", e.getMessage());
            context.status(500).json(String.format(errorResponse, "Server error during upload"));
            return;
        }

        Path targetPath = uploadDir.resolve(cleanFileName);

        // Check for conflict
        if (Files.exists(targetPath)) {
            logger.warn("Upload conflict: file '{}' already exists", cleanFileName);
            context.status(409).json(String.format(errorResponse, "File already exists: " + cleanFileName));
            return;
        }

        // Save file
        try (InputStream in = uploadedFile.content()) {
            Files.copy(in, targetPath);
        } catch (IOException e) {
            logger.error("Failed to save file '{}': {}", cleanFileName, e.getMessage());
            context.status(500).json(String.format(errorResponse, "Failed to store uploaded file"));
            return;
        }

        logger.info("Uploaded file: {}, size: {} bytes", cleanFileName, uploadedFile.size());

        Map<String, String> response = new HashMap<>();
        response.put("url", "/uploads/" + cleanFileName);
        context.status(200).json(response);
    }
}
