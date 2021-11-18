package threads;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

public class ServerThread extends Thread {
    private ServerSocket server;
    private HashSet<String> userNames;
    private HashSet<UserThread> userThreads;

    public ServerThread(ServerSocket server) {
        this.server = server;
        userNames = new HashSet<>();
        userThreads = new HashSet<>();
    }

    @Override
    public void run() {
        while (true) {
            Socket socket = null;
            try {
                socket = server.accept();
                UserThread newUser = new UserThread(socket, this);
                System.out.println(newUser + " connected");

                userThreads.add(newUser);
                newUser.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            System.out.println(userName + " quited");
        }
    }
}
