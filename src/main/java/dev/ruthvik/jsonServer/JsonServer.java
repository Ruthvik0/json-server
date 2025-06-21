package dev.ruthvik.jsonServer;

public class JsonServer {
    private final int port;
    private final DB db;

    public JsonServer(int port) {
        this.port = port;
        DBUtils.ensureDBFile();
        db = new DB();
    }

    public void run(){
        System.out.println(db.getEntities());
        System.out.printf("Json Server is running on port %s\n", port);
    }
}
