package server;

import threads.ServerThread;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Scanner;

public class Server {
    private final static int MAX_CONNECTIONS = 1024;
    private String host;
    private ServerThread serverThread;
    private int port;

    public Server(String host, int port) {
        this.port = port;
        this.host = host;
    }

    public void start() {
        try {
            ServerSocket server = new ServerSocket(port, MAX_CONNECTIONS, InetAddress.getByName(host));
            System.out.println("Chat server is listening on port " + port);
            serverThread = new ServerThread(server);
            serverThread.start();

            while (!serverThread.isInterrupted()) {
                readCommand();
            }
        } catch (IOException e) {
            System.out.println("Error while creating ServerSocket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void readCommand() {
        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNext()) {
            String text = scanner.nextLine();
            if (text.trim().length() > 0) {
                switch (text) {
                    case "/stop":
                        serverThread.closeServerThread();
                        break;
                    case "/users":
                        System.out.println(serverThread.getUserNames());
                        break;
                    default:
                        System.out.println("Incorrect command");
                        break;
                }
            }
        }
    }
}
