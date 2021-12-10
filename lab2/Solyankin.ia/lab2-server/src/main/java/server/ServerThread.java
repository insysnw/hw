package server;

import server.protocol.TFTPProtocol;
import server.protocol.TFTPServerRead;
import server.protocol.TFTPServerWrite;
import server.requests.TFTPServerRRQ;
import server.requests.TFTPServerWRQ;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ServerThread extends Thread {
    private final String host;
    private final int port;
    private DatagramSocket socket;

    public ServerThread(String host, int port) {
        this.port = port;
        this.host = host;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(port, InetAddress.getByName(host));
            System.out.println("Server now listening on " + host + ":" + port);
            System.out.println();

            while (true) {
                TFTPProtocol request = TFTPProtocol.receive(socket);
                if (request instanceof TFTPServerRead) {
                    System.out.println("New read request from " + request.getAddress());
                    new TFTPServerRRQ((TFTPServerRead) request);
                } else if (request instanceof TFTPServerWrite) {
                    System.out.println("New write request from " + request.getAddress());
                    new TFTPServerWRQ((TFTPServerWrite) request);
                }
            }
        } catch (IOException e) {
            System.out.println("Server terminated " + e.getMessage());
        }
    }

    public void closeServerThread() {
        socket.close();
        System.out.println("Server stopped");
        System.exit(-1);
    }
}
