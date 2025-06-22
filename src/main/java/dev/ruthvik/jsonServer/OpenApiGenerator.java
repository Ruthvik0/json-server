package dev.ruthvik.jsonServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

class OpenApiGenerator {
    private static final Logger logger = LoggerFactory.getLogger(OpenApiGenerator.class);
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public static void generateOpenApiDoc(List<String> entities, String path) {
        ObjectNode spec = mapper.createObjectNode();

        spec.put("openapi", "3.0.3");

        ObjectNode info = spec.putObject("info");
        info.put("title", "Dynamic JSON Server API");
        info.put("version", "1.0.0");
        info.put("description", "Auto-generated REST API for dynamic entities. Create entities and get instant CRUD operations!");

        ObjectNode paths = spec.putObject("paths");

        addNewEntityDoc(paths);

        entities.forEach(entity -> {
            String tag = entity.substring(0, 1).toUpperCase() + entity.substring(1);

            ObjectNode collectionPath = paths.putObject("/" + entity);

            ObjectNode getAll = collectionPath.putObject("get");
            getAll.put("summary", "Get All " + entity);
            getAll.put("description", "Retrieve all " + entity + " items");
            getAll.putArray("tags").add(tag);
            buildResponse(getAll.putObject("responses"), "200", "List of " + entity + " items", ResponseType.ARRAY);

            ObjectNode post = collectionPath.putObject("post");
            post.put("summary", "Create a new - " + entity);
            post.put("description", "Post a entity [" + entity + "]");
            post.putArray("tags").add(tag);

            ObjectNode createRequestBody = post.putObject("requestBody");
            createRequestBody.put("required", true);
            createRequestBody.put("description", "New " + entity + " item data");
            ObjectNode createContent = createRequestBody.putObject("content");
            ObjectNode createJson = createContent.putObject("application/json");
            ObjectNode createSchema = createJson.putObject("schema");
            createSchema.put("type", "object");

            buildResponse(post.putObject("responses"), "201", "Create a new entity " + entity, ResponseType.OBJECT);

            ObjectNode entityPathItem = paths.putObject("/" + entity + "/{id}");

            ObjectNode getOne = entityPathItem.putObject("get");
            getOne.put("summary", "Get " + entity + " by ID");
            getOne.put("description", "Retrieve a single " + entity + " item by ID");
            getOne.putArray("tags").add(tag);
            addIdParameter(getOne);
            buildResponse(getOne.putObject("responses"), "200", "Single " + entity + " item", ResponseType.OBJECT);

            ObjectNode delete = entityPathItem.putObject("delete");
            delete.put("summary", "Delete " + entity + " by ID");
            delete.put("description", "Delete a single " + entity + " item by ID");
            delete.putArray("tags").add(tag);
            addIdParameter(delete);
            buildResponse(delete.putObject("responses"), "204", "Entity deleted", ResponseType.OBJECT);

            ObjectNode put = entityPathItem.putObject("put");
            put.put("summary", "Update " + entity + " by ID");
            put.put("description", "Update a single " + entity + " item by ID");
            put.putArray("tags").add(tag);
            addIdParameter(put);
            ObjectNode updateRequestBody = put.putObject("requestBody");
            updateRequestBody.put("required", true);
            updateRequestBody.put("description", "Updated " + entity + " item data");
            ObjectNode updateContent = updateRequestBody.putObject("content");
            ObjectNode updateJson = updateContent.putObject("application/json");
            ObjectNode updateSchema = updateJson.putObject("schema");
            updateSchema.put("type", "object");
            buildResponse(put.putObject("responses"), "200", "Updated entity " + entity, ResponseType.OBJECT);
        });

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(path), spec);
            logger.info("OpenAPI spec generated successfully at: {}", path);
        } catch (Exception e) {
            logger.error("Failed to generate OpenAPI spec", e);
        }
    }

    private static void buildResponse(ObjectNode response, String status, String description, ResponseType responseType) {
        ObjectNode statusBlock = response.putObject(status);
        statusBlock.put("description", description);
        ObjectNode contentBlock = statusBlock.putObject("content");
        ObjectNode jsonBlock = contentBlock.putObject("application/json");
        ObjectNode schemaBlock = jsonBlock.putObject("schema");
        if (responseType.equals(ResponseType.ARRAY)) {
            schemaBlock.put("type", "array");
            schemaBlock.putObject("items").put("type", "object");
        } else {
            schemaBlock.put("type", "object");
        }
    }

    private static void addIdParameter(ObjectNode operation) {
        ObjectNode param = operation.putArray("parameters").addObject();
        param.put("name", "id");
        param.put("in", "path");
        param.put("required", true);
        ObjectNode schema = param.putObject("schema");
        schema.put("type", "string");
    }

    private static void addNewEntityDoc(ObjectNode paths){
        ObjectNode createEntityPath = paths.putObject("/entities");

        ObjectNode createEntityPost = createEntityPath.putObject("post");
        createEntityPost.put("summary", "Create a new dynamic entity");
        createEntityPost.put("description", "Creates a new entity name (table) that can be used to store data.");
        createEntityPost.putArray("tags").add("Meta");

        ObjectNode requestBody = createEntityPost.putObject("requestBody");
        requestBody.put("required", true);
        requestBody.put("description", "Entity name to be created");
        ObjectNode content = requestBody.putObject("content");
        ObjectNode appJson = content.putObject("application/json");
        ObjectNode schema = appJson.putObject("schema");
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("entityName").put("type", "string").put("example", "books");
        schema.putArray("required").add("entityName");

        // 201 response
        ObjectNode responses = createEntityPost.putObject("responses");

        ObjectNode resp201 = responses.putObject("201");
        resp201.put("description", "Entity successfully created");
        ObjectNode resp201Content = resp201.putObject("content");
        ObjectNode resp201AppJson = resp201Content.putObject("application/json");
        ObjectNode resp201Schema = resp201AppJson.putObject("schema");
        resp201Schema.put("type", "object");
        ObjectNode resp201Props = resp201Schema.putObject("properties");
        resp201Props.putObject("message").put("type", "string").put("example", "Entity added successfully");
        resp201Props.putObject("devTip").put("type", "string").put("example", "🚨 Restart the server to activate routes for this entity AND update OpenAPI docs");

        // 400 error response
        ObjectNode resp400 = responses.putObject("400");
        resp400.put("description", "Invalid or duplicate entity name");
        ObjectNode resp400Content = resp400.putObject("content");
        ObjectNode resp400AppJson = resp400Content.putObject("application/json");
        ObjectNode resp400Schema = resp400AppJson.putObject("schema");
        resp400Schema.put("type", "object");
        resp400Schema.putObject("properties").putObject("error").put("type", "string").put("example", "Entity already exists");
    }

    enum ResponseType {
        ARRAY,
        OBJECT,
    }
}
