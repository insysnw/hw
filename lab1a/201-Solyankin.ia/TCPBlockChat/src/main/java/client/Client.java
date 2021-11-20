package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Client {
    private String host;
    private int port;
    private User user;
    private Scanner scanner;
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;

    public Client(String host, int port) {
        this.port = port;
        this.host = host;
    }

    public void start() {
        try {
            scanner = new Scanner(System.in);
            socket = new Socket(host, port);
            System.out.println("Connected to the chat server");

            user = new User(null, false);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            System.out.println("Enter your name: ");
            while (!user.getNameStatus()) {
                String userName = scanner.nextLine();
                try {
                    output.writeUTF(userName);
                    String response = input.readUTF();
                    readMessage(response);
                    if (response.equals(userName + " joined")) {
                        user.setNameStatus(true);
                    }
                } catch (IOException e) {
                    System.out.println("Error while sending message: server closed");
                    closeThread();
                    System.exit(-1);
                }
            }

            SendMessageThread write = new SendMessageThread();
            ReceiveMessageThread read = new ReceiveMessageThread();

            write.start();
            read.start();

            while (true) {
                if (socket.isClosed()) {
                    write.interrupt();
                    read.interrupt();
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Server not found");
        }
    }

    private void readMessage(String response) {
        String[] partsMessage = response.split(" ", 2);
        String message = String.format("<%s>[%s]: %s", getCurrentTime(), partsMessage[0], partsMessage[1]);
        System.out.println(message);
    }

    private String getCurrentTime() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        return dateTimeFormatter.format(now);
    }

    private class SendMessageThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    String text = scanner.nextLine();
                    if (text != null) { // Add sending file
                        output.writeUTF(text);
                        if (text.toLowerCase().trim().equals("/quit")) {
                            closeThread();
                            interrupt();
                            System.exit(-1);
                            break;
                        }
                        output.flush();
                    }
                } catch (IOException e) {
                    System.out.println("Error while sending message: " + e.getMessage());
                    e.printStackTrace();
                    closeThread();
                    interrupt();
                    System.exit(-1);
                }
            }
        }
    }

    private class ReceiveMessageThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    String response = input.readUTF(); // Add receive file
                    if (response.toLowerCase().trim().equals("/quit")) {
                        System.out.println("Client closed");
                        closeThread();
                        interrupt();
                        System.exit(-1);
                        break;
                    } else {
                        readMessage(response);
                    }
                } catch (IOException e) {
                    System.out.println("Server closed");
                    closeThread();
                    interrupt();
                    System.exit(-1);
                    break;
                }
            }
        }
    }

    private void closeThread() {
        try {
            if (!socket.isClosed()) {
                output.close();
                input.close();
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing SendMessageThread: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
