package client;

import resources.Phrases;
import threads.ReceiveMessageThread;
import threads.SendMessageThread;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        user = new User(null, false);
    }

    public void start() {
        try {
            socket = new Socket(host, port);
            System.out.println(Phrases.CLIENT_WELCOME.getPhrase());

            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            enteringName();

            SendMessageThread write = new SendMessageThread(this, scanner, output);
            ReceiveMessageThread read = new ReceiveMessageThread(this, user, socket, input);

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
            System.out.println(Phrases.CLIENT_WELCOME_ERROR.getPhrase());
        }
    }

    private void enteringName() {
        System.out.println(Phrases.CLIENT_ENTER_NAME.getPhrase());
        while (!user.getNameStatus()) {
            String userName = scanner.nextLine();
            if (userNameIsSuitable(userName)) {
                try {
                    output.writeUTF(userName);
                    String response = input.readUTF();
                    readMessage(response);
                    user.setUserName(userName);
                    user.setNameStatus(true);
                } catch (IOException e) {
                    System.out.println(Phrases.CLIENT_ENTER_NAME_ERROR.getPhrase());
                    closeSocket();
                    System.exit(-1);
                }
            }
        }
    }

    private boolean userNameIsSuitable(String userName) {
        String regex = "[A-Za-z0-9\\n\\r\\t]*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(userName);
        if (matcher.matches()) {
            return true;
        } else {
            readMessage(Phrases.CLIENT_INCORRECT_NAME_ERROR.getPhrase());
            return false;
        }
    }

    public void readMessage(String response) {
        String[] partsMessage = response.split(" ", 2);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        String message = String.format("<%s>[%s]: %s", dateTimeFormatter.format(now), partsMessage[0], partsMessage[1]);
        System.out.println(message);
    }

    public void closeSocket() {
        try {
            if (!socket.isClosed()) {
                output.close();
                input.close();
                socket.close();
            }
        } catch (IOException e) {
            System.out.println(Phrases.CLIENT_CLOSE_SENDMESSAGETHREAD_ERROR.getPhrase() + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
