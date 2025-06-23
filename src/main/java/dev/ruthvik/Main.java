package dev.ruthvik;
import dev.ruthvik.jsonServer.JsonServer;

public class Main{
    public static void main(String[] args) {
        JsonServer.builder().build().run();
    }
}