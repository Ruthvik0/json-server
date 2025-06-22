package dev.ruthvik.jsonServer;

import dev.ruthvik.jsonServer.handler.*;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class JsonServer {
    private static final Logger logger = LoggerFactory.getLogger(JsonServer.class);
    private final int port;
    private final DB db;
    private final Javalin app;

    public JsonServer(int port) {
        this.port = port;
        DBUtils.ensureDBFile();
        db = new DB();
        app = Javalin.create(javalinConfig ->
                javalinConfig.staticFiles.add(staticFileConfig -> {
                    staticFileConfig.hostedPath = "/";
                    staticFileConfig.directory = "./public/";
                    staticFileConfig.location = Location.EXTERNAL;
                }
        ));
        OpenApiGenerator.generateOpenApiDoc(db.getAllEntityNames(), "./public/swagger/swagger.yml");
    }

    private static final String errorResponse = "{\"error\":\"%s\"}";

    public void run() {

        // Endpoint to create new entities
        app.post("/entities", ctx -> {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String entityName = (String) body.get("entityName");
            if (entityName == null || entityName.trim().isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST).json(String.format(errorResponse, "Invalid Entity Name"));
            } else if (db.getAllEntityNames().contains(entityName)) {
                ctx.status(HttpStatus.BAD_REQUEST).json(String.format(errorResponse, "Entity already exists"));
            } else {
                db.addEntity(entityName);

                Map<String, String> response = new HashMap<>();
                response.put("message", "Entity added successfully");
                response.put("devTip", "🚨 Restart the server to activate routes for this entity AND update OpenAPI docs");
                ctx.status(HttpStatus.CREATED).json(response);
                logger.info("Added new entity '{}'", entityName);
            }
        });

        db.getAllEntityNames().forEach((entityName) -> {
            app.get("/" + entityName, new GetAllEntitiesHandler(entityName, db));
            app.get("/" + entityName + "/{id}", new GetEntityHandler(entityName, db));
            app.post("/" + entityName, new PostEntityHandler(entityName, db));
            app.put("/" + entityName + "/{id}", new PutEntityHandler(entityName, db));
            app.delete("/" + entityName + "/{id}", new DeleteEntityHandler(entityName, db));
        });

        logger.info("Json Server is running on http://localhost:{}", port);
        logger.info("Swagger UI: http://localhost:{}/swagger", port);
        app.start(port);
    }
}
