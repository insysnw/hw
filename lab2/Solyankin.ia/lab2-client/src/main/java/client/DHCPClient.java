package client;

import client.protocol.DHCPMessage;
import client.protocol.Tools;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class DHCPClient extends Thread {
    private final String host;
    private final int port;
    private DatagramSocket clientSocket;

    private final byte[] mac = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    public DHCPClient(String host, int port) {
        this.host = host;
        this.port = port;
        try {
            clientSocket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("Error while creating clientSocket: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[576];
        int length = buffer.length;
        DatagramPacket receivePacketOffer = new DatagramPacket(buffer, length);

        DHCPDiscover();

        boolean ackReceived = false;
        while (!ackReceived) {
            try {
                clientSocket.setSoTimeout(5000);
                clientSocket.receive(receivePacketOffer);
                System.out.println("Receive offer response from server");
            } catch (IOException e) {
                System.out.println("Error while receive offer response: " + e.getMessage());
                System.exit(-1);
            }
            DHCPMessage offer = new DHCPMessage(receivePacketOffer.getData());
            System.out.println(offer);

            DHCPRequest(receivePacketOffer);

            DatagramPacket receivePacketAck = new DatagramPacket(buffer, length);
            try {
                clientSocket.receive(receivePacketAck);
                System.out.print("Receive ack response from server");
            } catch (IOException e) {
                System.out.println("Error while receive ack response: " + e.getMessage());
                e.printStackTrace();
                System.exit(-1);
            }

            DHCPMessage acknowledge = new DHCPMessage(receivePacketAck.getData());
            System.out.println(acknowledge);

            switch (acknowledge.getType()) {
                case DHCPMessage.DHCPACK:
                    System.out.println("IP Allocated to this client: ");
                    for (byte b : acknowledge.yiAddr)
                        System.out.print((b & 0xFF) + ".");
                    System.out.println();
                    ackReceived = true;
                    break;
                case DHCPMessage.DHCPNAK:
                    System.out.println("No IP allocated to this client");
                    break;
            }
            clientSocket.close();
        }
    }

    private void DHCPDiscover() {
        System.out.println("\nStart discover phase");
        DHCPMessage message = new DHCPMessage();
        message.op = 1;
        message.hType = 1;
        message.hLen = 6;
        message.hOps = 0;

        ByteBuffer hash = ByteBuffer.allocate(4);
        hash.putInt(message.hashCode());
        message.xId = hash.array();

        message.secs = new byte[]{0, 0};
        message.flags = new byte[]{0, 0};
        message.ciAddr = new byte[]{0, 0, 0, 0};
        message.yiAddr = new byte[]{0, 0, 0, 0};
        message.siAddr = new byte[]{0, 0, 0, 0};
        message.giAddr = new byte[]{0, 0, 0, 0};
        message.chAddr = mac;
        message.sName = new byte[64];
        message.file = new byte[128];
        message.magicCookie = DHCPMessage.COOKIE;
        message.addOption((byte) 53, (byte) 1, new byte[]{DHCPMessage.DHCPDISCOVER});
        message.addOption((byte) 50, (byte) 4, new byte[]{(byte) 192, (byte) 168, 1, 0});
        message.addOption((byte) 51, (byte) 4, Tools.toByteArray(30));
        message.addOption((byte) 255, (byte) 0, new byte[]{0});

        try {
            DatagramPacket discoverPacket = new DatagramPacket(message.getMessage(), message.getLength(), InetAddress./*getByAddress(host.getBytes())*/getByName(host), port);
            clientSocket.send(discoverPacket);
            System.out.println("DHCPDiscover sent");
        } catch (IOException e) {
            System.out.println("Error while sending discover packet: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void DHCPRequest(DatagramPacket offer) {
        System.out.println("\nStart request phase");

        DHCPMessage message = new DHCPMessage(offer.getData());
        message.clearOptions();

        message.op = 1;

        message.addOption((byte) 53, (byte) 1, new byte[]{DHCPMessage.DHCPREQUEST});
        message.addOption((byte) 50, (byte) 4, message.yiAddr);
        message.yiAddr = new byte[]{0, 0, 0, 0};
        message.addOption((byte) 54, (byte) 4, message.siAddr);
        message.addOption((byte) 51, (byte) 4, Tools.toByteArray(30));
        message.addOption((byte) 255, (byte) 0, new byte[]{0});

        try {
            int portServer = offer.getPort();
            DatagramPacket discoverPacket = new DatagramPacket(message.getMessage(), message.getLength(), InetAddress.getByAddress(message.siAddr), portServer);
            clientSocket.send(discoverPacket);
            System.out.println("DHCPRequest sent");
        } catch (IOException e) {
            System.out.println("Error while sending request packet: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
