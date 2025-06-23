package dev.ruthvik.jsonServer;

import dev.ruthvik.jsonServer.handler.*;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

/**
 * The main entry point for starting the JSON Server.
 * Supports dynamic CRUD entities, file upload, and OpenAPI docs.
 */
public class JsonServer {

    private static final Logger logger = LoggerFactory.getLogger(JsonServer.class);

    private static final long DEFAULT_MAX_REQUEST_SIZE = 5 * 1024 * 1024;       // 5 MB
    private static final long INTERNAL_MAX_THRESHOLD_SIZE = 50 * 1024 * 1024;   // 50 MB

    private static final Path DEFAULT_UPLOAD_DIR = Paths.get(System.getProperty("user.dir"), "uploads");
    private static final Path SWAGGER_DIR_PATH = Paths.get(System.getProperty("user.dir"), "src/main/resources/swagger", "swagger.yml");
    private static final Path GENERATED_SWAGGER_PATH = Paths.get(System.getProperty("user.dir"), "swagger-output", "swagger.yml");

    private final long FINAL_MAX_SIZE;
    private final String FINAL_UPLOAD_DIR;

    private final int port;
    private final DB db;
    private final Javalin app;

    /**
     * Entry point for building and configuring the JsonServer.
     * Example:
     * <pre>{@code
     * JsonServer server = JsonServer.builder()
     *     .port(8080)
     *     .maxRequestSize(10 * 1024 * 1024)
     *     .uploadDir(Paths.get("/custom/uploads"))
     *     .build();
     *
     * server.run();
     * }</pre>
     */
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

            config.staticFiles.add(staticFileConfig ->{
                staticFileConfig.hostedPath = "/swagger";
                staticFileConfig.directory = "/swagger";
                staticFileConfig.location = Location.CLASSPATH;
            });

            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.hostedPath = "/swagger/swaggerConfig";
                staticFileConfig.directory = GENERATED_SWAGGER_PATH.toString();
                staticFileConfig.location = Location.EXTERNAL;
            });

            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.hostedPath = "/uploads";
                staticFileConfig.directory = FINAL_UPLOAD_DIR;
                staticFileConfig.location = Location.EXTERNAL;
            });

            config.http.maxRequestSize = FINAL_MAX_SIZE;
            config.showJavalinBanner = false;
        });

        try {
            Files.createDirectories(GENERATED_SWAGGER_PATH.getParent());
        } catch (IOException e) {
            logger.error("Could not create swagger ouput directory: {}", FINAL_UPLOAD_DIR, e);
        }

        logger.info("Max request size set to {} bytes ({} MB)", maxRequestSize, maxRequestSize / (1024 * 1024));
        OpenApiGenerator.generateOpenApiDoc(db.getAllEntityNames(), GENERATED_SWAGGER_PATH.toString());

        try {
            Files.createDirectories(Paths.get(FINAL_UPLOAD_DIR));
        } catch (IOException e) {
            logger.error("Failed to create upload directory: {}", FINAL_UPLOAD_DIR, e);
        }
    }

    /**
     * Starts the JSON server on the configured port.
     * This will also initialize the dynamic endpoints and Swagger UI.
     */
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

    /**
     * Builder class for creating a customized instance of {@link JsonServer}.
     */
    public static class JsonServerBuilder {
        private int port = 7070;
        private long maxRequestSize = DEFAULT_MAX_REQUEST_SIZE;
        private Path uploadDir = DEFAULT_UPLOAD_DIR;

        /**
         * Sets the port number on which the server will run.
         *
         * @param port the port number
         * @return builder instance
         */
        public JsonServerBuilder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the maximum request size (in bytes).
         * Requests exceeding this size will be rejected.
         *
         * @param bytes the maximum request size in bytes
         * @return builder instance
         */
        public JsonServerBuilder maxRequestSize(long bytes) {
            this.maxRequestSize = bytes;
            return this;
        }

        /**
         * Sets the directory path to store uploaded image files.
         * This must be an external folder.
         *
         * @param dir path to the upload folder
         * @return builder instance
         */
        public JsonServerBuilder uploadDir(Path dir) {
            this.uploadDir = dir;
            return this;
        }

        /**
         * Builds the JsonServer instance using the specified settings.
         *
         * @return configured JsonServer instance
         */
        public JsonServer build() {
            return new JsonServer(port, maxRequestSize, uploadDir);
        }
    }
}
