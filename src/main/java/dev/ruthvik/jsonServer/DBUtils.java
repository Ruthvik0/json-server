package dev.ruthvik.jsonServer;

import java.io.*;

class DBUtils {
    private static final String DB_FILE_PATH = "./db.json";

    private DBUtils() {
    }

    public static void ensureDBFile() {
        File dbFile = new File(DB_FILE_PATH);
        if (!dbFile.exists()) {
            try {
                if (dbFile.createNewFile()) {
                    writeToDBFile("{}");
                    System.out.printf("Created DB file at path [%s]\n", DB_FILE_PATH);
                }
            } catch (IOException e) {
                System.err.printf("Could not create DB file at path [%s]%n", DB_FILE_PATH);
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("DB file found");
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
            return content.toString();
        } catch (IOException e) {
            System.err.printf("Error reading from DB file: %s%n", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void writeToDBFile(String content){
        try{
            File dbFile = new File(DB_FILE_PATH);
            BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile));
            writer.write(content);
            writer.flush();
        }catch (IOException e){
            System.err.printf("Error writing to DB File [%s]\n", e.getMessage());
        }
    }
}
