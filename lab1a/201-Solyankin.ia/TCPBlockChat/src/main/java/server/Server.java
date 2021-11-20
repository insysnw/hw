package server;

import threads.ServerThread;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class Server {
    private final static int MAX_CONNECTIONS = 1024;
    private String host;
    private int port;

    public Server(String host, int port) {
        this.port = port;
        this.host = host;
    }

    public void start() {
        try {
            ServerSocket server = new ServerSocket(port, MAX_CONNECTIONS, InetAddress.getByName(host));
            System.out.println("Chat server is listening on port " + port);
            ServerThread serverThread = new ServerThread(server);
            serverThread.start();

        } catch (IOException e) {
            System.out.println("Error in server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
