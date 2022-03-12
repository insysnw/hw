package protocol.interaction;

import protocol.messages.MessageFromClient;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DataReader {
    private final DataInputStream inputStream;

    public DataReader(InputStream inputStream) {
        this.inputStream = new DataInputStream(inputStream);
    }

    //Check
    public MessageFromClient read() {
        MessageFromClient messageFromClient = new MessageFromClient();
        try {
            String message = inputStream.readUTF();
            messageFromClient.setId(message.substring(0, 16));
            messageFromClient.setOpcode(message.substring(16, 24));
            messageFromClient.setArg1(message.substring(24, 56));
            messageFromClient.setArg2(message.substring(56, 88));
        } catch (IOException e) {
            System.out.println("Error while read message from client");
            e.printStackTrace();
        }
        return messageFromClient;
    }

    public boolean hasMessage() {
        try {
            return inputStream.available() > 0;
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    public void close() {
        try {
            inputStream.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
