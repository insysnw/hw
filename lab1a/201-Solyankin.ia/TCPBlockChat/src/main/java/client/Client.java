package client;

import resources.Phrases;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        if (matcher.matches()){
            return true;
        } else {
            readMessage(Phrases.CLIENT_INCORRECT_NAME_ERROR.getPhrase());
            return false;
        }
    }

    private class SendMessageThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    String message = scanner.nextLine();
                    if (message.split(" ", 2)[0].equals(Phrases.CLIENT_COMMAND_FILE.getPhrase())) {
                        sendFile(message.split(" ", 2)[1]);
                    } else {
                        output.writeUTF(message);
                        if (message.toLowerCase().trim().equals(Phrases.CLIENT_COMMAND_QUIT.getPhrase())) {
                            closeSocket();
                            break;
                        }
                        output.flush();
                    }
                } catch (IOException e) {
                    System.out.println(Phrases.SEND_MESSAGE_ERROR.getPhrase() + e.getMessage());
                    e.printStackTrace();
                    closeSocket();
                    System.exit(-1);
                }
            }
        }

        private void sendFile(String message) throws IOException {
            File file = new File(message);
            if (file.exists() && !file.isDirectory()) {
                byte[] fileBytes = Files.readAllBytes(Paths.get(file.getAbsolutePath().trim()));
                output.writeUTF(Phrases.CLIENT_COMMAND_FILE.getPhrase() + " " + fileBytes.length + " " + file.getName());
                output.flush();
                try {
                    output.write(fileBytes);
                    readMessage( Phrases.CLIENT_FILE_SENDING.getPhrase()+ fileBytes.length + " bytes)");
                    output.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println(Phrases.CLIENT_INVALID_FILE_ERROR.getPhrase());
            }
        }
    }

    private class ReceiveMessageThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    String response = input.readUTF();
                    if (response.contains(Phrases.SEND_FILE.getPhrase())) {
                        receiveFile(response);
                    } else {
                        if (response.toLowerCase().trim().equals(Phrases.SERVER_COMMAND_STOP_SERVER.getPhrase()) || !socket.isConnected()) {
                            System.out.println(Phrases.CLIENT_SERVER_CLOSED.getPhrase());
                            closeSocket();
                            System.exit(-1);
                            break;
                        } else {
                            readMessage(response);
                        }
                    }
                } catch (IOException e) {
                    System.out.println(Phrases.CLIENT_CLOSED.getPhrase());
                    System.exit(-1);
                    break;
                }
            }
        }

        private void receiveFile(String response) throws IOException {
            readMessage(response);
            String[] words = response.split(" ");
            int count = 4;
            StringBuilder fileName = new StringBuilder();
            while (!words[count].equals("(")) {
                fileName.append(words[count]).append(" ");
                count++;
            }
            String fileBytesLength = words[++count];
            byte[] fileBytes = new byte[Integer.parseInt(fileBytesLength)];
            for (int i = 0; i < fileBytes.length; i++) {
                byte aByte = input.readByte();
                fileBytes[i] = aByte;
            }
            saveFile(fileName.toString().trim(), fileBytes);
        }

        private void saveFile(String fileName, byte[] bytes) {
            String directoryPath = createDirectory();
            File file = new File(directoryPath + File.separator + fileName);
            try {
                if (file.createNewFile()) {
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(bytes);
                    fileOutputStream.close();
                    readMessage(Phrases.CLIENT_FILE_SAVED.getPhrase() + fileName);
                } else {
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(bytes);
                    fileOutputStream.close();
                    readMessage(Phrases.CLIENT_FILE_OVERWRITING.getPhrase() + fileName);
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        private String createDirectory() {
            File directory = new File(System.getProperty("user.home") + File.separator +
                    "Desktop" + File.separator + user.getUserName());
            if (!directory.exists()) {
                if (directory.mkdir()) {
                    readMessage(Phrases.CLIENT_DIRECTORY_CREATED.getPhrase() + directory.getAbsolutePath());
                }
            }
            return directory.getAbsolutePath();
        }
    }

    private void readMessage(String response) {
        String[] partsMessage = response.split(" ", 2);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        String message = String.format("<%s>[%s]: %s", dateTimeFormatter.format(now), partsMessage[0], partsMessage[1]);
        System.out.println(message);
    }

    private void closeSocket() {
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
