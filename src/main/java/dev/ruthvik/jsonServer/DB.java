package dev.ruthvik.jsonServer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DB {
    private static final Logger logger = LoggerFactory.getLogger(DB.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static Map<String, List<Map<String, Object>>> entities = new HashMap<>();

    DB() {
        loadDataFromDB();
    }

    public Map<String, List<Map<String, Object>>> getEntities() {
        return entities;
    }

    public List<String> getAllEntityNames(){
        return new ArrayList<>(entities.keySet());
    }

    public void addEntity(String entityName){
        entities.put(entityName, new ArrayList<>());
        saveDataToDB();
        logger.info("Entity '{}' added to database", entityName);
    }

    private void loadDataFromDB() {
        String dbRawData = DBUtils.readFromDBFile();
        try {
            entities = objectMapper.readValue(dbRawData, new TypeReference<Map<String, List<Map<String, Object>>>>() {});
            logger.info("Loaded entities from DB file");
        } catch (Exception e) {
            logger.warn("Could not parse values from DB JSON: {}", e.getMessage());
            entities = new HashMap<>();
        }
    }

    public List<Map<String, Object>> getEntity(String name) {
        return entities.getOrDefault(name, new ArrayList<>());
    }

    public Map<String, Object> getEntityItemById(String name, int id) {
        return getEntity(name).stream()
                .filter(obj -> obj.get("id") instanceof Number && ((Number) obj.get("id")).intValue() == id)
                .findFirst()
                .orElse(null);
    }

    public void addEntityItem(String name, Map<String, Object> item) {
        List<Map<String, Object>> list = entities.computeIfAbsent(name, k -> new ArrayList<>());
        int nextId = list.stream()
                .map(obj -> (Number) obj.getOrDefault("id", 0))
                .mapToInt(Number::intValue)
                .max()
                .orElse(0) + 1;
        item.put("id", nextId);
        list.add(item);
        saveDataToDB();
        logger.info("Added item with ID {} to entity '{}'", nextId, name);
    }

    public boolean updateEntityItem(String name, int id, Map<String, Object> newItem) {
        List<Map<String, Object>> list = getEntity(name);
        for (int i = 0; i < list.size(); i++) {
            if (((Number) list.get(i).get("id")).intValue() == id) {
                newItem.put("id", id);
                list.set(i, newItem);
                saveDataToDB();
                logger.info("Updated item with ID {} in entity '{}'", id, name);
                return true;
            }
        }
        logger.warn("Item with ID {} not found while updating '{}'", id, name);
        return false;
    }

    public boolean deleteEntityItem(String name, int id) {
        List<Map<String, Object>> list = getEntity(name);
        boolean removed = list.removeIf(item ->
                item.get("id") instanceof Number && ((Number) item.get("id")).intValue() == id);
        if (removed) {
            saveDataToDB();
            logger.info("Deleted item with ID {} from entity '{}'", id, name);
        } else {
            logger.warn("Item with ID {} not found while deleting '{}'", id, name);
        }
        return removed;
    }

    private void saveDataToDB() {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(entities);
            DBUtils.writeToDBFile(json);
            logger.debug("Saved DB state to file");
        } catch (Exception e) {
            logger.error("Could not write to DB file: {}", e.getMessage());
        }
    }
}
