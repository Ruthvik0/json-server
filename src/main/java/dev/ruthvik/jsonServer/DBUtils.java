package dev.ruthvik.jsonServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

class DBUtils {
    private static final Logger logger = LoggerFactory.getLogger(DBUtils.class);
    private static final String DB_FILE_PATH = "./db.json";

    private DBUtils() {}

    public static void ensureDBFile() {
        File dbFile = new File(DB_FILE_PATH);
        if (!dbFile.exists()) {
            try {
                if (dbFile.createNewFile()) {
                    writeToDBFile("{}");
                    logger.info("Created DB file at path [{}]", DB_FILE_PATH);
                }
            } catch (IOException e) {
                logger.error("Could not create DB file at path [{}]: {}", DB_FILE_PATH, e.getMessage());
                throw new RuntimeException(e);
            }
        } else {
            logger.info("DB file found at path [{}]", DB_FILE_PATH);
        }
    }

    public static String readFromDBFile() {
        File dbFile = new File(DB_FILE_PATH);
        try (BufferedReader reader = new BufferedReader(new FileReader(dbFile))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            logger.debug("Read data from DB file");
            return content.toString();
        } catch (IOException e) {
            logger.error("Error reading from DB file: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void writeToDBFile(String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DB_FILE_PATH))) {
            writer.write(content);
            writer.flush();
            logger.debug("Wrote data to DB file");
        } catch (IOException e) {
            logger.error("Error writing to DB file: {}", e.getMessage());
        }
    }
}