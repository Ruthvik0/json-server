package dev.ruthvik.jsonServer.handler;

import dev.ruthvik.jsonServer.DB;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PutEntityHandler implements Handler {

    private final String entityName;
    private final DB db;

    public PutEntityHandler(String entityName, DB db) {
        this.entityName = entityName;
        this.db = db;
    }

    @Override
    public void handle(@NotNull Context ctx) {
        String errorResponse = "{\"error\":\"%s\"}";

        int id;
        try {
            id = Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST).json(String.format(errorResponse, "Invalid ID"));
            return;
        }

        try {
            Map<String, Object> updatedItem = ctx.bodyAsClass(Map.class);
            boolean updated = db.updateEntityItem(entityName, id, updatedItem);
            if (!updated) {
                ctx.status(HttpStatus.NOT_FOUND).json(String.format(errorResponse, "Resource not found"));
                return;
            }
            ctx.status(HttpStatus.OK).json(updatedItem);
        } catch (Exception e) {
            ctx.status(HttpStatus.BAD_REQUEST).json(String.format(errorResponse, "Invalid request body"));
        }
    }
}
