package server.threads;

import server.resources.Phrases;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ServerThread extends Thread {
    private ServerSocket server;
    private Set<UserThread> userThreads;

    public ServerThread(ServerSocket server) {
        this.server = server;
        userThreads = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public void run() {
        while (!server.isClosed()) {
            try {
                Socket socket = server.accept();
                UserThread userThread = new UserThread(socket, this);
                userThread.start();
            } catch (IOException ignored) {
            }
        }
    }

    void addUserThreads(UserThread userThread) {
        userThreads.add(userThread);
    }

    void removeUser(UserThread userThread) {
        userThreads.remove(userThread);
    }

    public void closeServerThread() {
        System.out.println(Phrases.SERVER_STOPPED.getPhrase());
        userThreads.clear();
        System.exit(-1);
    }
}
