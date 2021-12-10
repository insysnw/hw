package server.requests;

import server.protocol.*;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Paths;

public class TFTPServerWRQ extends Thread {
    private DatagramSocket socket;
    private InetAddress host;
    private int port;
    private FileOutputStream outputFile;
    private TFTPProtocol request;
    private int timeoutLimit = 5;
    private File saveFile;
    private String fileName;

    /**
     * Handling a write request
     *
     * @param request - Write request
     */
    public TFTPServerWRQ(TFTPServerWrite request) {
        try {
            this.request = request;
            socket = new DatagramSocket();
            socket.setSoTimeout(1000);
            host = request.getAddress();
            port = request.getPort();
            fileName = request.fileName();

            saveFile = new File(Paths.get(".").toAbsolutePath().normalize().toFile() + File.separator + fileName);

            if (!saveFile.exists()) {
                outputFile = new FileOutputStream(saveFile);
                TFTPServerAck acknowledge = new TFTPServerAck(0);
                acknowledge.send(host, port, socket);
                this.start();
            } else {
                try {
                    TFTPServerError error = new TFTPServerError(6, "File already exists");
                    error.send(host, port, socket);
                } catch (Exception ignored) {
                }
                throw new TFTPServerException("Error while handling write request: file exists");
            }
        } catch (Exception e) {
            TFTPServerError error = new TFTPServerError(1, e.getMessage());
            try {
                error.send(host, port, socket);
            } catch (Exception ignored) {
            }
            System.out.println("Client error: " + e.getMessage());
            System.out.println();
        }
    }

    public void run() {
        if (request instanceof TFTPServerWrite) {
            try {
                int blockNumber = 1;
                int bytesOut = 512;
                for (; bytesOut == 512; blockNumber++) {
                    while (timeoutLimit != 0) {
                        try {
                            TFTPProtocol receive = TFTPProtocol.receive(socket);
                            if (receive instanceof TFTPServerError) {
                                TFTPServerError error = (TFTPServerError) receive;
                                throw new TFTPServerException(error.message());
                            } else if (receive instanceof TFTPServerData) {
                                TFTPServerData data = (TFTPServerData) receive;
                                if (data.blockNumber() != blockNumber) {
                                    throw new SocketTimeoutException();
                                }
                                bytesOut = data.write(outputFile);
                                TFTPServerAck acknowledge = new TFTPServerAck(blockNumber);
                                acknowledge.send(host, port, socket);
                                break;
                            }
                        } catch (SocketTimeoutException t2) {
                            System.out.println("Time out, resend ack");
                            TFTPServerAck acknowledge = new TFTPServerAck(blockNumber - 1);
                            acknowledge.send(host, port, socket);
                            timeoutLimit--;
                        }
                    }
                    if (timeoutLimit == 0) {
                        throw new Exception("Connection failed");
                    }
                }
                System.out.println("Transfer completed.(Client " + host + ")");
                System.out.println("Filename: " + fileName);
                System.out.println();
            } catch (Exception e) {
                try {
                    TFTPServerError error = new TFTPServerError(1, e.getMessage());
                    error.send(host, port, socket);
                    System.out.println("Client failed:  " + e.getMessage());
                    saveFile.delete();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
