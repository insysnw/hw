package threads;

import client.Client;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WriteThread extends Thread {
    private PrintWriter writer;
    private Socket socket;
    private Client client;
    private String userName;

    public WriteThread(Socket socket, Client client, String userName) {
        this.socket = socket;
        this.client = client;
        this.userName = userName;
        try {
            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);
        } catch (IOException ex) {
            System.out.println("Error getting output stream: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void run() {
        Console console = System.console();
//        String userName = console.readLine("\nEnter your name: ");
//        chatClient.setUserName(userName);
        writer.println(userName);

        String text;

        do {
//            text = console.readLine(getMessageDescription(userName));
            try {
                text = new BufferedReader(new InputStreamReader(System.in)).readLine();
                if (!text.isEmpty())
                    writer.println(text);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

        } while (!text.equals("bye"));

        try {
            socket.close();
        } catch (IOException ex) {

            System.out.println("Error writing to server: " + ex.getMessage());
        }
    }

    private String getMessageDescription(String userName) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        return "<" + dateTimeFormatter.format(now) + ">" + "[" + userName + "]: ";
    }
}

