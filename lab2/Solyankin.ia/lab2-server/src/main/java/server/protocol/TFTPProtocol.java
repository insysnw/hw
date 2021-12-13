package server.protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class TFTPProtocol {
    public static int TFTPPackLen = 516;

    static final short TFTP_RRQ = 1;
    static final short TFTP_WRQ = 2;
    static final short TFTP_DATA = 3;
    static final short TFTP_ACK = 4;
    static final short TFTP_ERROR = 5;

    static final int OPCODE_OFFSET = 0;
    static final int FILE_OFFSET = 2;
    static final int BLOCK_OFFSET = 2;
    static final int NUM_OFFSET = 2;
    static final int DATA_OFFSET = 4;
    static final int MSG_OFFSET = 4;

    byte[] message;
    int length;
    private InetAddress host;
    private int port;

    public TFTPProtocol() {
        message = new byte[TFTPPackLen];
        length = TFTPPackLen;
    }

    /**
     * The method in which the received request is processed and, depending on its Opcode, an appropriate class is created to process the request
     *
     * @param socket - DatagramSocket, from which we will read requests
     * @return - currPacket, inherited from the current class that describes the incoming request
     */
    public static TFTPProtocol receive(DatagramSocket socket) throws IOException {
        TFTPProtocol input = new TFTPProtocol();
        TFTPProtocol protocol = new TFTPProtocol();

        if (!socket.isClosed()) {
            DatagramPacket inputPacket = new DatagramPacket(input.message, input.length);
            socket.receive(inputPacket);

            switch (input.get(0)) {
                case TFTP_RRQ:
                    protocol = new TFTPServerRead();
                    break;
                case TFTP_WRQ:
                    protocol = new TFTPServerWrite();
                    break;
                case TFTP_DATA:
                    protocol = new TFTPServerData();
                    break;
                case TFTP_ACK:
                    protocol = new TFTPServerAck();
                    break;
                case TFTP_ERROR:
                    protocol = new TFTPServerError();
                    break;
            }

            protocol.message = input.message;
            protocol.length = inputPacket.getLength();
            protocol.host = inputPacket.getAddress();
            protocol.port = inputPacket.getPort();

            return protocol;
        } else {
            return new TFTPServerError(2, "Socket closed");
        }
    }

    public InetAddress getAddress() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getLength() {
        return length;
    }

    /**
     * Method that sends a datagram packet from the current socket
     *
     * @param host   - Host from which the request was received
     * @param port   - Port from which the request was received
     * @param socket - Socket from which will send the datagram packet created with the entered parameters
     * @throws IOException - Exception while send datagram
     */
    public void send(InetAddress host, int port, DatagramSocket socket) throws IOException {
        socket.send(new DatagramPacket(message, length, host, port));
    }

    /**
     * Getting 2 bytes from the message at position index
     *
     * @param index - The position from which 2 bytes will be output from the message
     * @return - Value of 2 bytes from the message
     */
    public int get(int index) {
        return (message[index] & 0xff) << 8 | message[index + 1] & 0xff;
    }

    /**
     * Retrieving bytes from the message at position index until byte equals end
     *
     * @param index - The position at which to output bytes from the message
     * @param end   - Byte value upon reaching which byte reading will stop
     * @return - Value of 2 bytes from the message
     */
    public String get(int index, byte end) {
        StringBuilder result = new StringBuilder();
        while (message[index] != end) result.append((char) message[index++]);
        return result.toString();
    }

    /**
     * Methods to put opcode, blkNum or error code into the message
     *
     * @param index - The position from which bytes will be inserted into the message
     * @param value - The value to be added to the message
     */
    public void put(int index, short value) {
        message[index++] = (byte) (value >>> 8);  // first byte
        message[index] = (byte) (value % 256);    // last byte
    }

    /**
     * Put the filename and mode into the 'message' at 'index' follow by byte "end"
     *
     * @param index - The position from which bytes will be inserted into the message
     * @param value - The string whose value will be added to the message
     * @param end   - Byte that will mark the end of the message
     */
    public void put(int index, String value, byte end) {
        value.getBytes(0, value.length(), message, index);
        message[index + value.length()] = end;
    }
}
