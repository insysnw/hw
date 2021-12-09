package server;

import java.util.Scanner;

public class TFTPServer {
    private ServerThread serverThread;
    private String host;
    private int port;

    public TFTPServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        serverThread = new ServerThread(host, port);
        serverThread.start();
        while (!serverThread.isInterrupted()) {
            readCommand();
        }
    }

    private void readCommand() {
        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNext()) {
            String text = scanner.nextLine();
            if (text.trim().length() > 0) {
                switch (text.trim().toLowerCase()) {
                    case "/stop":
                        serverThread.closeServerThread();
                        break;
                    default:
                        System.out.println("Incorrect command");
                        break;
                }
            }
        }
    }
}
