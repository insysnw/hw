package server.requests;

import server.protocol.*;

import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Paths;

public class TFTPServerRRQ extends Thread {
    private DatagramSocket socket;
    private InetAddress host;
    private int port;
    private FileInputStream source;
    private TFTPProtocol request;
    private int timeoutLimit = 5;
    private String fileName;

    /**
     * Handling a read request
     *
     * @param request - Read request
     */
    public TFTPServerRRQ(TFTPServerRead request) {
        try {
            this.request = request;
            socket = new DatagramSocket();
            socket.setSoTimeout(1000);
            host = request.getAddress();
            port = request.getPort();
            fileName = request.fileName();

            File srcFile = new File(Paths.get(".").toAbsolutePath().normalize().toFile() + File.separator + fileName);

            if (srcFile.exists() && srcFile.isFile() && srcFile.canRead()) {
                source = new FileInputStream(srcFile);
                this.start();
            } else {
                try {
                    TFTPServerError error = new TFTPServerError(1, "File not exists");
                    error.send(host, port, socket);
                } catch (Exception ignored) {
                }
                throw new TFTPServerException("Error while handling write request: file dont exists");
            }
        } catch (Exception e) {
            TFTPServerError error = new TFTPServerError(1, e.getMessage());
            try {
                error.send(host, port, socket);
            } catch (Exception ignored) {
            }
            System.out.println("Client error:  " + e.getMessage());
            System.out.println();
        }
    }

    public void run() {
        int bytesRead = TFTPProtocol.TFTPPackLen;
        if (request instanceof TFTPServerRead) {
            try {
                int blkNum = 1;
                for (; bytesRead == TFTPProtocol.TFTPPackLen; blkNum++) {
                    TFTPServerData outputRequest = new TFTPServerData(blkNum, source);
                    bytesRead = outputRequest.getLength();
                    outputRequest.send(host, port, socket);

                    //wait for the correct ack. if incorrect, retry up to 5 times
                    while (timeoutLimit != 0) {
                        try {
                            TFTPProtocol receive = TFTPProtocol.receive(socket);
                            if (!(receive instanceof TFTPServerAck)) {
                                throw new Exception("Error: incorrect request");
                            }
                            TFTPServerAck acknowledge = (TFTPServerAck) receive;
                            if (acknowledge.blockNumber() != blkNum) {
                                throw new SocketTimeoutException("last packet lost, resend packet");
                            }
                            break;
                        } catch (SocketTimeoutException t) {
                            System.out.println("Resent blk " + blkNum);
                            timeoutLimit--;
                            outputRequest.send(host, port, socket);
                        }
                    }
                    if (timeoutLimit == 0) {
                        throw new Exception("connection failed");
                    }
                }
                System.out.println("Transfer completed.(Client " + host + ")");
                System.out.println("Filename: " + fileName);
                System.out.println();
            } catch (Exception e) {
                try {
                    TFTPServerError ePak = new TFTPServerError(1, e.getMessage());
                    ePak.send(host, port, socket);
                    System.out.println("Client failed:  " + e.getMessage());
                } catch (Exception ignored) {
                }
            }
        }
    }
}
