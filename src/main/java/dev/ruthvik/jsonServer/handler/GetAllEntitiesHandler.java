package dev.ruthvik.jsonServer.handler;

import dev.ruthvik.jsonServer.DB;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class GetAllEntitiesHandler implements Handler {

    private final String entityName;
    private final DB db;

    public GetAllEntitiesHandler(String entityName, DB db) {
        this.entityName = entityName;
        this.db = db;
    }

    @Override
    public void handle(@NotNull Context context) {
        context.status(HttpStatus.OK).json(db.getEntities().getOrDefault(entityName, new ArrayList<>()));
    }
}
