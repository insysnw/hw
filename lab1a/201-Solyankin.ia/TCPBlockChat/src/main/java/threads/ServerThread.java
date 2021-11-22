package threads;

import resources.Phrases;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ServerThread extends Thread {
    private ServerSocket server;
    private final Set<String> userNames;
    private final Set<UserThread> userThreads;

    public ServerThread(ServerSocket server) {
        this.server = server;
        userNames = Collections.synchronizedSet(new HashSet<>());
        userThreads = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public void run() {
        while (!server.isClosed()) {
            try {
                Socket socket = server.accept();
                UserThread newUser = new UserThread(socket, this);
                newUser.start();
            } catch (IOException ignored) {
            }
        }
    }

    public void broadcast(String message) {
        for (UserThread user : userThreads) {
            user.sendMessage(message);
        }
    }

    public void broadcastBytes(byte[] bytes) {
        for (UserThread user : userThreads) {
            user.sendBytes(bytes);
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

    public void closeServerThread() {
        try {
            userNames.clear();
            for (UserThread userThread : userThreads) {
                userThread.sendMessage(Phrases.SERVER_COMMAND_STOP_SERVER.getPhrase());
                userThread.closeUserThread();
            }
            userThreads.clear();
            server.close();
            System.out.println(Phrases.SERVER_STOPPED.getPhrase());
            System.exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
