package dev.ruthvik.jsonServer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class DB {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static Map<String, List<Map<String, Object>>> entities = new HashMap<>();

    public DB() {
        loadDataFromDB();
    }

    public Map<String, List<Map<String, Object>>> getEntities() {
        return entities;
    }

    private void loadDataFromDB() {
        String dbRawData = DBUtils.readFromDBFile();
        try {
            entities = objectMapper.readValue(dbRawData, new TypeReference<Map<String, List<Map<String, Object>>>>() {});
        } catch (Exception e) {
            System.err.println("Could not parse values from json: " + e.getMessage());
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
    }

    public boolean updateEntityItem(String name, int id, Map<String, Object> newItem) {
        List<Map<String, Object>> list = getEntity(name);
        for (int i = 0; i < list.size(); i++) {
            if (((Number) list.get(i).get("id")).intValue() == id) {
                newItem.put("id", id);
                list.set(i, newItem);
                saveDataToDB();
                return true;
            }
        }
        return false;
    }

    public boolean deleteEntityItem(String name, int id) {
        List<Map<String, Object>> list = getEntity(name);
        boolean removed = list.removeIf(item ->
                item.get("id") instanceof Number && ((Number) item.get("id")).intValue() == id);
        if (removed) saveDataToDB();
        return removed;
    }

    private void saveDataToDB() {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(entities);
            DBUtils.writeToDBFile(json);
        } catch (Exception e) {
            System.err.println("Could not write to DB file: " + e.getMessage());
        }
    }
}
