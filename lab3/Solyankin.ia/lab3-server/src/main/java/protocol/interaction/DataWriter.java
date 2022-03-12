package protocol.interaction;

import protocol.messages.MessageFromServer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DataWriter {
    private final DataOutputStream outputStream;

    public DataWriter(OutputStream outputStream) {
        this.outputStream = new DataOutputStream(outputStream);
    }

    //Check
    public void write(MessageFromServer message) {
        try {
            outputStream.writeUTF(message.toMessage());
        } catch (IOException e) {
            System.out.println("Error while write message");
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            outputStream.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
