package server;

import resources.Phrases;
import threads.ServerThread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Scanner;

public class Server {
    private ServerThread serverThread;
    private final String host;
    private final int port;

    public Server(String host, int port) {
        this.port = port;
        this.host = host;
    }

    public void start() throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.socket().bind(new InetSocketAddress(host, port));
        Selector selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println(Phrases.SERVER_WELCOME.getPhrase() + port);

        serverThread = new ServerThread(server, selector);
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
