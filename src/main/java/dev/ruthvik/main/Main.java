package dev.ruthvik.main;

import dev.ruthvik.jsonServer.JsonServer;

public class Main {
    public static void main(String[] args) {

        JsonServer jsonServer = new JsonServer(3000);
        jsonServer.run();
    }
}
