package dev.ruthvik.jsonServer.handler;

import dev.ruthvik.jsonServer.DB;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class CreateNewEntity implements Handler {
    
    private final DB db;
    private final Logger logger = LoggerFactory.getLogger(CreateNewEntity.class);

    public CreateNewEntity(DB db) {
        this.db = db;
    }
    
    @Override
    public void handle(@NotNull Context context) {
        final String errorResponse = "{\"error\":\"%s\"}";

        Map<String, Object> body = context.bodyAsClass(Map.class);
        String entityName = (String) body.get("entityName");
        entityName = entityName.trim().replaceAll("[^a-zA-Z0-9_-]", "");
        if (entityName.trim().isEmpty()) {
            context.status(HttpStatus.BAD_REQUEST).json(String.format(errorResponse, "Invalid Entity Name"));
        } else if (db.getAllEntityNames().contains(entityName)) {
            context.status(HttpStatus.BAD_REQUEST).json(String.format(errorResponse, "Entity already exists"));
        } else {
            db.addEntity(entityName);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Entity added successfully");
            response.put("devTip", "🚨 Restart the server to activate routes for this entity AND update OpenAPI docs");
            context.status(HttpStatus.CREATED).json(response);
            logger.info("Added new entity '{}'", entityName);
        }
    }
}
