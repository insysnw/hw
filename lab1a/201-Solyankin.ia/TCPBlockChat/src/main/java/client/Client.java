package client;

import threads.ReadThread;
import threads.WriteThread;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {
    private String host;
    private int port;
    private String userName;
    private final Scanner scanner;

    public Client(String host, int port) {
        this.port = port;
        this.host = host;
        this.scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    }

    public void start() {
        try {
            System.out.println("Enter your name: ");
            String userName = scanner.nextLine();
            setUserName(userName);

            Socket socket = new Socket(host, port);
            System.out.println("Connected to the chat server");

            new WriteThread(socket, this, userName).start();
            new ReadThread(socket, this).start();

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O Error: " + ex.getMessage());
        }
    }

    public void setUserName(String userName){
        this.userName = userName;
    }
}

