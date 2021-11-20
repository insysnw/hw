package server;

import threads.UserThread;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Server {
    private final static int MAX_CONNECTIONS = 1024;
    private final Set<String> userNames;
    private final Set<UserThread> userThreads;
    private String host;
    private int port;
    private Socket socket;

    public Server(String host, int port) {
        this.port = port;
        this.host = host;
        userNames = Collections.synchronizedSet(new HashSet<>());
        userThreads = Collections.synchronizedSet(new HashSet<>());
    }

    public void start() {
        try {
            ServerSocket server = new ServerSocket(port, MAX_CONNECTIONS, InetAddress.getByName(host));
            System.out.println("Chat server is listening on port " + port);

            while (true) {
                socket = server.accept();
                UserThread newUser = new UserThread(socket, this);
                newUser.start();
            }
        } catch (IOException e) {
            System.out.println("Error in server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void broadcast(String message, UserThread excludeUser) {
        for (UserThread user : userThreads) {
//            if (user != excludeUser) {
            user.sendMessage(message);
//            }
        }
    }

    public void removeUser(String userName, UserThread userThread) {
        boolean removed = userNames.remove(userName);
        if (removed) {
            userThreads.remove(userThread);
        }
    }

    public Set<String> getUserNames() {
        return userNames;
    }

    public void putUserNames(String userName) {
        userNames.add(userName);
    }

    public void putUserThreads(UserThread userThread) {
        userThreads.add(userThread);
    }
}
