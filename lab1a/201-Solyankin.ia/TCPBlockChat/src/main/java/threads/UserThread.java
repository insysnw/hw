package threads;

import client.User;
import server.Server;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserThread extends Thread {
    private User user;
    private DataInputStream input;
    private Server server;
    private DataOutputStream output;

    public UserThread(User user, DataInputStream input, DataOutputStream output, Server server) {
        this.user = user;
        this.input = input;
        this.output = output;
        this.server = server;
    }

    public static String time() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        return dateTimeFormatter.format(now);
    }

    @Override
    public void run() {
        try {
            String userName = user.getUserName();

            String serverMessage = "New user connected: " + userName;
            server.broadcast(serverMessage, this);

            String clientMessages = "";

            while (true) {
                clientMessages = input.readUTF();
                if (!clientMessages.trim().toLowerCase().equals("/quit")) {
                    serverMessage = "[" + userName + "]: " + clientMessages;
                    server.broadcast(serverMessage, this);
                    System.out.println(getMessageDescription(userName) + clientMessages);
                } else {
                    output.writeUTF(clientMessages);
                    break;
                }
            }

            server.removeUser(userName, this);

            serverMessage = userName + " has quited.";
            server.broadcast(serverMessage, this);

        } catch (IOException e) {
            System.out.println("Error in UserThread: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void sendMessage(String message) throws IOException {
        output.writeUTF(message);
    }

    private String getMessageDescription(String userName) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        return "<" + dateTimeFormatter.format(now) + ">" + "[" + userName + "]: ";
    }
}
