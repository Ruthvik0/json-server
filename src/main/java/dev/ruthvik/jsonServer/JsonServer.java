package dev.ruthvik.jsonServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ruthvik.jsonServer.handler.*;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

public class JsonServer {
    private final int port;
    private final DB db;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Javalin app;

    public JsonServer(int port) {
        this.port = port;
        DBUtils.ensureDBFile();
        db = new DB();
        app = Javalin.create();
    }

    private static final String errorResponse = "{\"error\":\"%s\"}";

    public void run() {

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
                response.put("devTip", "🚨 Restart the server to activate routes for this entity");
                ctx.status(HttpStatus.CREATED).json(response);
            }
        });

        db.getAllEntityNames().forEach((entityName) -> {

            // GET /entity
            app.get("/" + entityName, new GetAllEntitiesHandler(entityName, db));

            // GET /entity/{id}
            app.get("/" + entityName + "/{id}", new GetEntityHandler(entityName, db));

            // POST /entity
            app.post("/" + entityName, new PostEntityHandler(entityName, db));

            // PUT /entity/{id}
            app.put("/" + entityName + "/{id}", new PutEntityHandler(entityName, db));

            // DELETE /entity/{id}
            app.delete("/" + entityName + "/{id}", new DeleteEntityHandler(entityName, db));

        });

        System.out.printf("Json Server is running on http://localhost:%s%n", port);
        app.start(port);
    }
}
