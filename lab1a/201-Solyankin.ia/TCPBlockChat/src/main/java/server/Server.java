package server;

import client.User;
import threads.UserThread;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Server {
    private final static int MAX_CONNECTIONS = 1024;
    //    private final ServerSocket server;
    private final Set<String> userNames;
    private final Set<UserThread> userThreads;
    private String host;
    private int port;
    private DataInputStream input;
    private DataOutputStream output;
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

                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());
                User user = new User(null, false);

                do {
                    String userName = input.readUTF();
                    if (!userNames.contains(userName)) {
                        user = new User(userName, true);
                        userNames.add(userName);
                        output.writeUTF("Connect successful");
                        System.out.println(userName + " connected");
                    } else {
                        output.writeUTF("Connect failed, name already exist");
                    }
                } while (!user.getNameStatus());

                UserThread newUser = new UserThread(user, input, output, this);
                userThreads.add(newUser);
                newUser.start();
            }
        } catch (IOException e) {
            System.out.println("Error in server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void broadcast(String message, UserThread excludeUser) throws IOException {
        for (UserThread user : userThreads) {
            if (user != excludeUser) {
                user.sendMessage(message);
            }
        }
    }

    public void removeUser(String userName, UserThread userThread) {
        boolean removed = userNames.remove(userName);
        if (removed) {
            userThreads.remove(userThread);
            System.out.println(userName + " quited");
        }
    }
}
