package dev.ruthvik.jsonServer.handler;

import dev.ruthvik.jsonServer.DB;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PostEntityHandler implements Handler {

    private final String entityName;
    private final DB db;

    public PostEntityHandler(String entityName, DB db) {
        this.entityName = entityName;
        this.db = db;
    }

    @Override
    public void handle(@NotNull Context context) {
        Map<String, Object> item = context.bodyAsClass(Map.class);
        db.addEntityItem(entityName, item);
        context.status(HttpStatus.CREATED).json(item);
    }
}
