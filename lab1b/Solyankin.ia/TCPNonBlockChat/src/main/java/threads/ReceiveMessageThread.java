package threads;

import client.Client;
import client.User;
import resources.Phrases;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ReceiveMessageThread extends Thread {
    private Client client;
    private DataInputStream input;
    private User user;
    private Socket socket;

    public ReceiveMessageThread(Client client, User user, Socket socket, DataInputStream input) {
        this.client = client;
        this.user = user;
        this.socket = socket;
        this.input = input;
    }

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
                        client.closeSocket();
                        System.exit(-1);
                        break;
                    } else {
                        client.readMessage(response);
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
        client.readMessage(response);
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
                client.readMessage(Phrases.CLIENT_FILE_SAVED.getPhrase() + fileName);
            } else {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(bytes);
                fileOutputStream.close();
                client.readMessage(Phrases.CLIENT_FILE_OVERWRITING.getPhrase() + fileName);
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
                client.readMessage(Phrases.CLIENT_DIRECTORY_CREATED.getPhrase() + directory.getAbsolutePath());
            }
        }
        return directory.getAbsolutePath();
    }
}
