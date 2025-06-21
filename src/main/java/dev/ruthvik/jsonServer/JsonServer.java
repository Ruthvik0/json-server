package dev.ruthvik.jsonServer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class JsonServer {
    private final int port;
    private final DB db;
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonServer(int port) {
        this.port = port;
        DBUtils.ensureDBFile();
        db = new DB();
    }

    public void run() {
        port(port);

        post("/entities", (request, response) -> {
            try {
                Map<String, Object> body = mapper.readValue(request.body(), Map.class);
                String entityName = (String) body.get("entityName");

                if (entityName == null || entityName.trim().isEmpty()) {
                    response.status(400);
                    return errorResponse("Invalid entity name");
                } else if (db.getAllEntityNames().contains(entityName)) {
                    response.status(400);
                    return errorResponse("Entity already exists");
                }

                db.addEntity(entityName);
                response.status(201);
                response.type("application/json");
                return successResponse("Entity added successfully");

            } catch (Exception e) {
                response.status(400);
                return errorResponse("Invalid request body");
            }
        });


        db.getEntities().forEach((entityName, entityData) -> {
            // GET /entity
            get("/" + entityName, (request, response) -> {
                response.type("application/json");
                return mapper.writeValueAsString(entityData);
            });

            // GET /entity/:id
            get("/" + entityName + "/:id", (request, response) -> {
                int id;
                try {
                    id = Integer.parseInt(request.params(":id"));
                } catch (NumberFormatException e) {
                    response.status(400);
                    response.type("application/json");
                    return errorResponse("Invalid Id");
                }

                Map<String, Object> item = db.getEntityItemById(entityName, id);
                if (item == null) {
                    response.status(404);
                    response.type("application/json");
                    return errorResponse("Resource not found");
                }

                response.type("application/json");
                return mapper.writeValueAsString(item);
            });

            // POST /entity
            post("/" + entityName, (request, response) -> {
                try {
                    Map<String, Object> item = mapper.readValue(request.body(), Map.class);
                    db.addEntityItem(entityName, item);
                    response.status(201);
                    response.type("application/json");
                    return mapper.writeValueAsString(item);
                } catch (Exception e) {
                    response.status(400);
                    return errorResponse("Invalid request body");
                }
            });

            // PUT /entity/:id
            put("/" + entityName + "/:id", (request, response) -> {
                int id;
                try {
                    id = Integer.parseInt(request.params(":id"));
                } catch (NumberFormatException e) {
                    response.status(400);
                    return errorResponse("Invalid ID");
                }

                try {
                    Map<String, Object> updatedItem = mapper.readValue(request.body(), Map.class);
                    boolean updated = db.updateEntityItem(entityName, id, updatedItem);
                    if (!updated) {
                        response.status(404);
                        return errorResponse("Resource not found");
                    }
                    response.type("application/json");
                    return mapper.writeValueAsString(updatedItem);
                } catch (Exception e) {
                    response.status(400);
                    return errorResponse("Invalid request body");
                }
            });

            // DELETE /entity/:id
            delete("/" + entityName + "/:id", (request, response) -> {
                int id;
                try {
                    id = Integer.parseInt(request.params(":id"));
                } catch (NumberFormatException e) {
                    response.status(400);
                    return errorResponse("Invalid ID");
                }

                boolean deleted = db.deleteEntityItem(entityName, id);
                if (!deleted) {
                    response.status(404);
                    return errorResponse("Resource not found");
                }

                response.status(204); // No Content
                return "";
            });

        });

        System.out.printf("Json Server is running on http://localhost:%s%n", port);
    }

    private String errorResponse(String error) {
        try {
            Map<String, String> map = new HashMap<>();
            map.put("error", error);
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String successResponse(String message) {
        try {
            Map<String, String> map = new HashMap<>();
            map.put("message", message);
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
