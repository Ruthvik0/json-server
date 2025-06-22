package dev.ruthvik.jsonServer.handler;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UploadedFile;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class FileUploadHandler implements Handler {

    private final Logger logger = LoggerFactory.getLogger(FileUploadHandler.class);

    @Override
    public void handle(@NotNull Context context) {
        String errorResponse = "{\"error\":\"%s\"}";

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

        // ✅ Light sanitization
        String cleanFileName = originalName.trim()
                .replaceAll("[\\\\/]", "")      // remove slashes
                .replaceAll("\\p{Cntrl}", "") // remove control characters
                .replaceAll("\\s+", "_");       // replace spaces with underscores (optional)

        Path uploadDir = Paths.get("./public/uploads");
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            logger.error("Failed to create upload directory: {}", e.getMessage());
            context.status(500).json(String.format(errorResponse, "Server error during upload"));
            return;
        }

        Path targetPath = uploadDir.resolve(cleanFileName);

        // Conflict check
        if (Files.exists(targetPath)) {
            logger.warn("Upload conflict: file '{}' already exists", cleanFileName);
            context.status(409).json(String.format(errorResponse, "File already exists: " + cleanFileName));
            return;
        }

        // Save file
        try {
            Files.copy(uploadedFile.content(), targetPath);
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
