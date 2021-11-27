package client;

import threads.UserThread;

public class Client {
    private String host;
    private int port;

    public Client(String host, int port) {
        this.port = port;
        this.host = host;
    }

    public void start() {
        UserThread userThread = new UserThread(host, port);
        userThread.start();
    }
}
