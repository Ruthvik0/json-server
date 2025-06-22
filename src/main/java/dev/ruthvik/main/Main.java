package dev.ruthvik.main;

import dev.ruthvik.jsonServer.JsonServer;

import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {

        JsonServer jsonServer = JsonServer.builder()
                .port(3000)
                .uploadDir(Paths.get("uploads"))
                .build();
        jsonServer.run();
    }
}
