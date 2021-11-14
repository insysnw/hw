package server;

import threads.UserThread;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

public class Server {
    private final static int MAX_CONNECTIONS = 1024;
    private String host;
    private int port;
    private HashSet<String> userNames;
    private HashSet<UserThread> userThreads;

    public Server(String host, int port) {
        this.port = port;
        this.host = host;
        userNames = new HashSet<>();
        userThreads = new HashSet<>();
    }

    public void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(port, MAX_CONNECTIONS, InetAddress.getByName(host));
            System.out.println("Chat server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New user connected");

                UserThread newUser = new UserThread(socket, this);
                userThreads.add(newUser);
                newUser.start();
            }
        } catch (IOException e) {
            System.out.println("Error in server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void broadcast(String message, UserThread excludeUser) {
        for (UserThread user : userThreads) {
            if (user!=excludeUser) {
                user.sendMessage(message);
            }
        }
    }

    public void addUserName(String userName) {
        userNames.add(userName);
    }

    public void removeUser(String userName, UserThread userThread) {
        boolean removed = userNames.remove(userName);
        if (removed) {
            userThreads.remove(userThread);
            System.out.println("The user " + userName + " quited");
        }
    }
}
