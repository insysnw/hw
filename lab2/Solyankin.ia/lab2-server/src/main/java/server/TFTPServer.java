package server;

import server.protocol.TFTPProtocol;
import server.protocol.TFTPServerRead;
import server.protocol.TFTPServerWrite;
import server.requests.TFTPServerRRQ;
import server.requests.TFTPServerWRQ;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class TFTPServer {
    private String host;
    private int port;

    public TFTPServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        try {
            DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName(host));

            while (true) {
                TFTPProtocol request = TFTPProtocol.receive(socket);
                if (request instanceof TFTPServerRead) {
                    System.out.println("Read Request from " + request.getAddress());
                    TFTPServerRRQ r = new TFTPServerRRQ((TFTPServerRead) request);
                } else if (request instanceof TFTPServerWrite) {
                    System.out.println("Write Request from " + request.getAddress());
                    TFTPServerWRQ w = new TFTPServerWRQ((TFTPServerWrite) request);
                }
            }
        } catch (SocketException e) {
            System.out.println("Server terminated(SocketException) " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Server terminated(IOException)" + e.getMessage());
        }
    }
}
