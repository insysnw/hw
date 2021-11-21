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
            while (!user.getNameStatus()) {
                userName = input.readUTF();
                if (!server.getUserNames().contains(userName)) {
                    user = new User(userName, true);
                    server.putUserNames(userName);
                    server.putUserThreads(this);
                    System.out.println(String.format("<%s>[%s]: %s", getCurrentTime(), userName, "joined"));
                    server.broadcast(userName + " joined");
                } else {
                    output.writeUTF("Server Connect failed, name already exist");
                }
            }

            while (true) {
                String clientMessages = input.readUTF();
                if (!clientMessages.trim().toLowerCase().equals("/quit")) {
                    if (clientMessages.split(" ", 3)[0].equals("/file")) {
                        String byteLength = clientMessages.split(" ", 3)[1];
                        String fileName = clientMessages.split(" ", 3)[2];
                        byte[] fileBytes = new byte[Integer.parseInt(byteLength)];
                        System.out.println(String.format("<%s>[%s]: %s", getCurrentTime(), userName, "Sent file: " + fileName));
                        for (int i = 0; i < fileBytes.length; i++) {
                            byte aByte = input.readByte();
                            fileBytes[i] = aByte;
                        }
                        server.broadcast(userName + " Sent the file: " + fileName + " ( " + fileBytes.length + " bytes)");
                        server.broadcastBytes(fileBytes);
                    } else {
                        server.broadcast(userName + " " + clientMessages);
                        String message = String.format("<%s>[%s]: %s", getCurrentTime(), userName, clientMessages);
                        System.out.println(message);
                    }
                } else {
                    server.removeUser(userName, this);
                    System.out.println(String.format("<%s>[%s]: %s", getCurrentTime(), userName, "quited"));
                    server.broadcast(userName + " quited");
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println(userName + " quited");
            server.removeUser(userName, this);
        }

    }

    public void sendMessage(String message) {
        try {
            output.writeUTF(message);
        } catch (IOException e) {
            System.out.println("Error while send message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendBytes(byte[] bytes) {
        try {
            output.write(bytes);
        } catch (IOException e) {
            System.out.println("Error while send message: " + e.getMessage());
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
