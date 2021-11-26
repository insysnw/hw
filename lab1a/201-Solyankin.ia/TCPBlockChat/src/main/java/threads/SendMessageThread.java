package threads;

import client.Client;
import resources.Phrases;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class SendMessageThread extends Thread {
    private Client client;
    private Scanner scanner;
    private DataOutputStream output;

    public SendMessageThread(Client client, Scanner scanner, DataOutputStream output) {
        this.client = client;
        this.scanner = scanner;
        this.output = output;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String message = scanner.nextLine();
                if (message.split(" ", 2)[0].equals(Phrases.CLIENT_COMMAND_FILE.getPhrase())) {
                    sendFile(message.split(" ", 2)[1]);
                } else {
                    if (!message.isEmpty()) {
                        output.writeUTF(message);
                        if (message.toLowerCase().trim().equals(Phrases.CLIENT_COMMAND_QUIT.getPhrase())) {
                            client.closeSocket();
                            break;
                        }
                        output.flush();
                    } else {
                        client.readMessage(Phrases.CLIENT_EMPTY_MESSAGE_ERROR.getPhrase());
                    }
                }
            } catch (IOException e) {
                System.out.println(Phrases.SEND_MESSAGE_ERROR.getPhrase() + e.getMessage());
                e.printStackTrace();
                client.closeSocket();
                System.exit(-1);
            }
        }
    }

    private void sendFile(String message) throws IOException {
        File file = new File(message);
        if (file.length() > 5 * 1024 * 1024) {
            System.out.println(Phrases.CLIENT_INVALID_FILE_SIZE.getPhrase());
        } else {
            if (file.exists() && !file.isDirectory()) {
                byte[] fileBytes = Files.readAllBytes(Paths.get(file.getAbsolutePath().trim()));
                output.writeUTF(Phrases.CLIENT_COMMAND_FILE.getPhrase() + " " + fileBytes.length + " " + file.getName());
                output.flush();
                try {
                    output.write(fileBytes);
                    client.readMessage(Phrases.CLIENT_FILE_SENDING.getPhrase() + fileBytes.length + " bytes)");
                    output.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println(Phrases.CLIENT_INVALID_FILE_ERROR.getPhrase());
            }
        }
    }
}
