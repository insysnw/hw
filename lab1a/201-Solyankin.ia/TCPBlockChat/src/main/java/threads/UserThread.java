package threads;

import client.User;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserThread extends Thread {
    private ServerThread server;
    private DataInputStream input;
    private DataOutputStream output;
    private String userName;

    public UserThread(Socket socket, ServerThread server) {
        try {
            this.input = new DataInputStream(socket.getInputStream());
            this.output = new DataOutputStream(socket.getOutputStream());
            this.server = server;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            User user = new User(null, false);
            do {
                userName = input.readUTF();
                if (!server.getUserNames().contains(userName)) {
                    user = new User(userName, true);
                    server.putUserNames(userName);
                    server.putUserThreads(this);
                    String welcomeMessage = userName + " has joined";
                    System.out.println(welcomeMessage);
                    server.broadcast(userName + " joined", this);
                } else {
                    output.writeUTF("Server Connect failed, name already exist");
                }
            } while (!user.getNameStatus());

            while (true) {
                String clientMessages = input.readUTF();
                String serverMessage;
                if (!clientMessages.trim().toLowerCase().equals("/quit")) {
                    serverMessage = userName + " " + clientMessages;
                    server.broadcast(serverMessage, this);
                    String message = String.format("<%s>[%s]: %s", getCurrentTime(), userName, clientMessages);
                    System.out.println(message);
                } else {
                    output.writeUTF(clientMessages);
                    server.removeUser(userName, this);
                    System.out.println(userName + " has quited");
                    serverMessage = userName + " has quited";
                    server.broadcast(serverMessage, this);
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println(userName + " urgently quited");
            server.removeUser(userName, this);
            server.broadcast(userName + " has quited", this);
        }
    }

    public void sendMessage(String message) {
        try {
            output.writeUTF(message);
        } catch (IOException e) {
            System.out.println("Error while send message" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getCurrentTime() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        return dateTimeFormatter.format(now);
    }

    public void closeUserThread() {
        try {
            input.close();
            output.close();
            interrupt();
        } catch (IOException e) {
            System.out.println("Error while closing userThread: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
