package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
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

            WriteThread write = new WriteThread();
            ReadThread read = new ReadThread();

            write.start();
            read.start();

            while (true) {
                if (socket.isClosed()) {
                    write.interrupt();
                    read.interrupt();
                    break;
                }
            }
        } catch (UnknownHostException e) {
            System.out.println("Server not found: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }
    }

    private class WriteThread extends Thread {

        @Override
        public void run() {
            enterName();

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

        private void enterName() {
            System.out.println("Enter your name: ");
            while (!user.getNameStatus()) {
                String userName = scanner.nextLine();
                try {
                    output.writeUTF(userName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ReadThread extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    String response = input.readUTF(); // Add receive file
                    if (response.toLowerCase().trim().equals("/quit")) {
                        System.out.println("Server closed");
                        closeThread();
                        interrupt();
                        System.exit(-1);
                        break;
                    } else {
                        sendMessage(response);
                    }
                } catch (IOException e) {
                    System.out.println("Error while reading message: " + e.getMessage());
                    e.printStackTrace();
                    closeThread();
                    interrupt();
                    System.exit(-1);
                    break;
                }
            }
        }

        private void sendMessage(String text) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalDateTime now = LocalDateTime.now();
            System.out.println(("\n<" + dateTimeFormatter.format(now) + ">" + text).trim());
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
            System.out.println("Error closing WriteThread: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
