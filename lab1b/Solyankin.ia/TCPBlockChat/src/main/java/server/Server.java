package server;

import resources.Phrases;
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
            System.out.println(Phrases.SERVER_WELCOME.getPhrase() + port);
            serverThread = new ServerThread(server);
            serverThread.start();

            while (!serverThread.isInterrupted()) {
                readCommand();
            }
        } catch (IOException e) {
            System.out.println(Phrases.SERVER_WELCOME_ERROR.getPhrase() + e.getMessage());
            e.printStackTrace();
        }
    }

    private void readCommand() {
        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNext()) {
            String text = scanner.nextLine();
            if (text.trim().length() > 0) {
                Phrases phrase = Phrases.fromString(text);
                switch (phrase) {
                    case SERVER_COMMAND_STOP_SERVER:
                        serverThread.closeServerThread();
                        break;
                    case SERVER_COMMAND_SHOW_USERS:
                        System.out.println(serverThread.getUserNames());
                        break;
                    case SERVER_INCORRECT_COMMAND_ERROR:
                        System.out.println(Phrases.SERVER_INCORRECT_COMMAND_ERROR.getPhrase());
                        break;
                }
            }
        }
    }
}
