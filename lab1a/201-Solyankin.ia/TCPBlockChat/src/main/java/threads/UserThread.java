package threads;

import client.User;
import resources.Phrases;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserThread extends Thread {
    private ServerThread server;
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private String userName;

    public UserThread(Socket socket, ServerThread server) {
        try {
            this.socket = socket;
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
            gettingUserName(user);

            while (true) {
                String clientMessages = input.readUTF();
                if (!clientMessages.trim().toLowerCase().equals(Phrases.CLIENT_COMMAND_QUIT.getPhrase())) {
                    if (clientMessages.split(" ", 3)[0].equals(Phrases.CLIENT_COMMAND_FILE.getPhrase())) {
                        fileProcessing(clientMessages);
                    } else {
                        server.broadcast(userName + " " + clientMessages);
                        String message = String.format("<%s>[%s]: %s", getCurrentTime(), userName, clientMessages);
                        System.out.println(message);
                    }
                } else {
                    server.removeUser(userName, this);
                    System.out.println(String.format("<%s>[%s]:%s", getCurrentTime(), userName, Phrases.USER_QUITED.getPhrase()));
                    server.broadcast(userName + Phrases.USER_QUITED.getPhrase());
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println(userName + Phrases.USER_QUITED.getPhrase());
            server.removeUser(userName, this);
            server.broadcast(userName + Phrases.USER_QUITED.getPhrase());
        }
    }

    private void gettingUserName(User user) throws IOException {
        while (!user.getNameStatus()) {
            userName = input.readUTF();
            if (!server.getUserNames().contains(userName)) {
                user = new User(userName, true);
                server.putUserNames(userName);
                server.putUserThreads(this);
                System.out.println(String.format("<%s>[%s]:%s", getCurrentTime(), userName, Phrases.USER_JOINED.getPhrase()));
                server.broadcast(userName + Phrases.USER_JOINED.getPhrase());
            } else {
                output.writeUTF(Phrases.SERVER_CONNECT_ERROR.getPhrase());
            }
        }
    }

    private void fileProcessing(String clientMessages) throws IOException {
        String byteLength = clientMessages.split(" ", 3)[1];
        String fileName = clientMessages.split(" ", 3)[2];
        byte[] fileBytes = new byte[Integer.parseInt(byteLength)];
        System.out.println(String.format("<%s>[%s]: %s", getCurrentTime(), userName, Phrases.SEND_FILE.getPhrase() + fileName));
        for (int i = 0; i < fileBytes.length; i++) {
            byte aByte = input.readByte();
            fileBytes[i] = aByte;
        }
        server.broadcast(userName + " " + Phrases.SEND_FILE.getPhrase() + fileName + " ( " + fileBytes.length + " bytes)");
        server.broadcastBytes(fileBytes);
    }

    public void sendMessage(String message) {
        if (!socket.isClosed()) {
            try {
                output.writeUTF(message);
            } catch (IOException e) {
                System.out.println(Phrases.SEND_MESSAGE_ERROR.getPhrase() + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void sendBytes(byte[] bytes) {
        if (!socket.isClosed()) {
            try {
                output.write(bytes);
            } catch (IOException e) {
                System.out.println(Phrases.SERVER_SEND_BYTES_ERROR.getPhrase() + e.getMessage());
                e.printStackTrace();
            }
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
            System.out.println(Phrases.SERVER_CLOSE_USERTHREAD_ERROR.getPhrase() + e.getMessage());
            e.printStackTrace();
        }
    }
}
