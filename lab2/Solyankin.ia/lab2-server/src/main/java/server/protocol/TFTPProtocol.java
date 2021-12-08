package server.protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class TFTPProtocol {
    public static int maxTFTPPackLen = 516;
    public static int maxTFTPData = 512;

    // Tftp opcodes
    protected static final short tftpRRQ = 1;
    protected static final short tftpWRQ = 2;
    protected static final short tftpDATA = 3;
    protected static final short tftpACK = 4;
    protected static final short tftpERROR = 5;

    // Packet Offsets
    protected static final int opOffset = 0;
    protected static final int fileOffset = 2;
    protected static final int blkOffset = 2;
    protected static final int numOffset = 2;
    protected static final int dataOffset = 4;
    protected static final int msgOffset = 4;

    protected byte[] message;
    protected int length;

    // Address info (required for replies)
    protected InetAddress host;
    protected int port;

    public TFTPProtocol() {
        message = new byte[maxTFTPPackLen];
        length = maxTFTPPackLen;
    }

    /**
     * @param socket
     * @return
     * @throws IOException
     */
    public static TFTPProtocol receive(DatagramSocket socket) throws IOException {
        TFTPProtocol input = new TFTPProtocol();
        TFTPProtocol currPacket = new TFTPProtocol();

        DatagramPacket inputPacket = new DatagramPacket(input.message, input.length);
        socket.receive(inputPacket);

        switch (input.get(0)) {
            case tftpRRQ:
                currPacket = new TFTPServerRead();
                break;
            case tftpWRQ:
                currPacket = new TFTPServerWrite();
                break;
            case tftpDATA:
                currPacket = new TFTPServerData();
                break;
            case tftpACK:
                currPacket = new TFTPServerAck();
                break;
            case tftpERROR:
                currPacket = new TFTPServerError();
                break;
        }

        currPacket.message = input.message;
        currPacket.length = inputPacket.getLength();
        currPacket.host = inputPacket.getAddress();
        currPacket.port = inputPacket.getPort();

        return currPacket;
    }

    //Method to send packet
    public void send(InetAddress ip, int port, DatagramSocket s) throws IOException {
        s.send(new DatagramPacket(message, length, ip, port));
    }

    // DatagramPacket like methods
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
     * @param index
     * @return
     */
    protected int get(int index) {
        return (message[index] & 0xff) << 8 | message[index + 1] & 0xff;
    }

    /**
     * @param at
     * @param del
     * @return
     */
    protected String get(int at, byte del) {
        StringBuffer result = new StringBuffer();
        while (message[at] != del) result.append((char) message[at++]);
        return result.toString();
    }

    /**
     * Methods to put opcode, blkNum, error code into the byte array 'message'
     *
     * @param at
     * @param value
     */
    protected void put(int at, short value) {
        message[at++] = (byte) (value >>> 8);  // first byte
        message[at] = (byte) (value % 256);    // last byte
    }

    /**
     * Put the filename and mode into the 'message' at 'at' follow by byte "del"
     *
     * @param at
     * @param value
     * @param del
     */
    @SuppressWarnings("deprecation")
    protected void put(int at, String value, byte del) {
        value.getBytes(0, value.length(), message, at);
        message[at + value.length()] = del;
    }
}
