package dev.ruthvik.jsonServer.handler;

import dev.ruthvik.jsonServer.DB;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class GetEntityHandler implements Handler {

    private final String entityName;
    private final DB db;

    public GetEntityHandler(String entityName, DB db) {
        this.entityName = entityName;
        this.db = db;
    }

    @Override
    public void handle(@NotNull Context context) {
        int entityId = 0;
        String errorResponse = "{\"error\":\"%s\"}";
        try {
            entityId = Integer.parseInt(context.pathParam("id"));
        } catch (NumberFormatException e) {
            context.status(HttpStatus.BAD_REQUEST).json(String.format(errorResponse, "Invalid Id"));
        }
        Map<String, Object> item = db.getEntityItemById(entityName, entityId);
        if (item == null) {
            context.status(HttpStatus.BAD_REQUEST).json(String.format(errorResponse, "Resource not found"));
        } else {
            context.status(HttpStatus.OK).json(item);

        }
    }
}