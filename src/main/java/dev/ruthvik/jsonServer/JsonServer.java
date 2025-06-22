package dev.ruthvik.jsonServer;

import dev.ruthvik.jsonServer.handler.*;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class JsonServer {

    private static final Logger logger = LoggerFactory.getLogger(JsonServer.class);

    // Limits
    private static final long DEFAULT_MAX_REQUEST_SIZE = 5 * 1024 * 1024;       // 5 MB
    private static final long INTERNAL_MAX_THRESHOLD_SIZE = 50 * 1024 * 1024;   // 50 MB

    // Defaults
    private static final Path DEFAULT_UPLOAD_DIR = Paths.get(System.getProperty("user.dir"), "uploads");
    private static final Path SWAGGER_DIR_PATH = Paths.get(System.getProperty("user.dir"), "src/main/resources/swagger", "swagger.yml");

    // Final Config
    private final long FINAL_MAX_SIZE;
    private final String FINAL_UPLOAD_DIR;

    private final int port;
    private final DB db;
    private final Javalin app;

    // === Constructors ===

    public static JsonServerBuilder builder() {
        return new JsonServerBuilder();
    }

    private JsonServer(int port, long maxRequestSize, Path uploadDir) {
        this.port = port;

        if (maxRequestSize > INTERNAL_MAX_THRESHOLD_SIZE) {
            logger.warn("maxRequestSize {} exceeds allowed limit {}. Reverting to default {}.",
                    maxRequestSize, INTERNAL_MAX_THRESHOLD_SIZE, DEFAULT_MAX_REQUEST_SIZE);
            maxRequestSize = DEFAULT_MAX_REQUEST_SIZE;
        }

        FINAL_MAX_SIZE = maxRequestSize;
        FINAL_UPLOAD_DIR = uploadDir != null ? uploadDir.toAbsolutePath().toString() : DEFAULT_UPLOAD_DIR.toString();

        DBUtils.ensureDBFile();
        db = new DB();

        this.app = Javalin.create(config -> {
            // Serve Swagger UI (classpath)
            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.hostedPath = "/swagger";
                staticFileConfig.directory = "/swagger";
                staticFileConfig.location = Location.CLASSPATH;
            });

            // Serve uploaded files (external)
            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.hostedPath = "/uploads";
                staticFileConfig.directory = FINAL_UPLOAD_DIR;
                staticFileConfig.location = Location.EXTERNAL;
            });

            config.http.maxRequestSize = FINAL_MAX_SIZE;
            config.showJavalinBanner = false;
        });

        logger.info("Max request size set to {} bytes ({} MB)", maxRequestSize, maxRequestSize / (1024 * 1024));
        OpenApiGenerator.generateOpenApiDoc(db.getAllEntityNames(), SWAGGER_DIR_PATH.toString());

        try {
            Files.createDirectories(Paths.get(FINAL_UPLOAD_DIR));
        } catch (IOException e) {
            logger.error("Failed to create upload directory: {}", FINAL_UPLOAD_DIR, e);
        }
    }

    // === Main server startup ===

    public void run() {
        app.post("/entities", new CreateNewEntity(db));

        app.get("/entities", ctx -> {
            HashMap<String, List<String>> response = new HashMap<>();
            response.put("entities", db.getAllEntityNames());
            ctx.status(HttpStatus.OK).json(response);
        });

        app.post("/upload", new FileUploadHandler(FINAL_MAX_SIZE, Paths.get(FINAL_UPLOAD_DIR)));

        db.getAllEntityNames().forEach(entityName -> {
            app.get("/" + entityName, new GetAllEntitiesHandler(entityName, db));
            app.get("/" + entityName + "/{id}", new GetEntityHandler(entityName, db));
            app.post("/" + entityName, new PostEntityHandler(entityName, db));
            app.put("/" + entityName + "/{id}", new PutEntityHandler(entityName, db));
            app.delete("/" + entityName + "/{id}", new DeleteEntityHandler(entityName, db));
        });

        app.get("/swagger", ctx -> ctx.redirect("/swagger/index.html"));

        logger.info("Json Server is running on http://localhost:{}", port);
        logger.info("Swagger UI available at http://localhost:{}/swagger", port);
        app.start(port);
    }

    // === Builder ===

    public static class JsonServerBuilder {
        private int port = 7070;
        private long maxRequestSize = DEFAULT_MAX_REQUEST_SIZE;
        private Path uploadDir = DEFAULT_UPLOAD_DIR;

        public JsonServerBuilder port(int port) {
            this.port = port;
            return this;
        }

        public JsonServerBuilder maxRequestSize(long bytes) {
            this.maxRequestSize = bytes;
            return this;
        }

        public JsonServerBuilder uploadDir(Path dir) {
            this.uploadDir = dir;
            return this;
        }

        public JsonServer build() {
            return new JsonServer(port, maxRequestSize, uploadDir);
        }
    }
}