package threads;

import client.User;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ServerThread extends Thread {
    private final ServerSocket server;
    private final Set<String> userNames;
    private final Set<UserThread> userThreads;
    private DataInputStream input;
    private DataOutputStream output;
    private Socket socket;

    public ServerThread(ServerSocket server) {
        this.server = server;
        userNames = Collections.synchronizedSet(new HashSet<>());
        userThreads = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public void run() {
        while (true) {
            try {
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


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcast(String message, UserThread excludeUser) throws IOException {
        for (UserThread user : userThreads) {
            if (user!=excludeUser) {
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

    private void stopServerThread() {
        try {
            if (!socket.isClosed()) {
                output.close();
                input.close();
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing ServerThread: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
