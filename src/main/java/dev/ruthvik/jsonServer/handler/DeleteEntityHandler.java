package dev.ruthvik.jsonServer.handler;

import dev.ruthvik.jsonServer.DB;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

public class DeleteEntityHandler implements Handler {

    private final String entityName;
    private final DB db;

    public DeleteEntityHandler(String entityName, DB db) {
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

        boolean deleted = db.deleteEntityItem(entityName, id);
        if (!deleted) {
            ctx.status(HttpStatus.NOT_FOUND).json(String.format(errorResponse, "Resource not found"));
        } else {
            ctx.status(HttpStatus.NO_CONTENT); // 204
        }
    }
}
