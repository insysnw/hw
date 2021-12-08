package server.requests;

import server.protocol.*;

import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class TFTPServerRRQ extends Thread {
    private DatagramSocket sock;
    private InetAddress host;
    private int port;
    private FileInputStream source;
    private TFTPProtocol req;
    private int timeoutLimit = 5;
    private String fileName;

    public TFTPServerRRQ(TFTPServerRead request) {
        try {
            req = request;
            //open new socket with random port num for tranfer
            sock = new DatagramSocket();
            sock.setSoTimeout(1000);
            fileName = request.fileName();

            host = request.getAddress();
            port = request.getPort();

            //create file object in parent folder
            File srcFile = new File("../" + fileName);
            /*System.out.println("procce checking");*/
            //check file
            if (srcFile.exists() && srcFile.isFile() && srcFile.canRead()) {
                source = new FileInputStream(srcFile);
                this.start(); //open new thread for transfer
            } else
                throw new TFTPServerException("access violation");

        } catch (Exception e) {
            TFTPServerError ePak = new TFTPServerError(1, e.getMessage()); // error code 1
            try {
                ePak.send(host, port, sock);
            } catch (Exception ignored) {
            }

            System.out.println("Client start failed:  " + e.getMessage());
        }
    }

    //everything is fine, open new thread to transfer file
    public void run() {
        int bytesRead = TFTPProtocol.maxTFTPPackLen;
        // handle read request
        if (req instanceof TFTPServerRead) {
            try {
                for (int blkNum = 1; bytesRead == TFTPProtocol.maxTFTPPackLen; blkNum++) {
                    TFTPServerData outPak = new TFTPServerData(blkNum, source);
                    /*System.out.println("send block no. " + outPak.blockNumber()); */
                    bytesRead = outPak.getLength();
                    /*System.out.println("bytes sent:  " + bytesRead);*/
                    outPak.send(host, port, sock);
                    /*System.out.println("current op code  " + outPak.get(0)); */

                    //wait for the correct ack. if incorrect, retry up to 5 times
                    while (timeoutLimit != 0) {
                        try {
                            TFTPProtocol ack = TFTPProtocol.receive(sock);
                            if (!(ack instanceof TFTPServerAck)) {
                                throw new Exception("Client failed");
                            }
                            TFTPServerAck a = (TFTPServerAck) ack;

                            if (a.blockNumber() != blkNum) { //check ack
                                throw new SocketTimeoutException("last packet lost, resend packet");
                            }
                            /*System.out.println("confirm blk num " + a.blockNumber()+" from "+a.getPort());*/
                            break;
                        } catch (SocketTimeoutException t) {//resend last packet
                            System.out.println("Resent blk " + blkNum);
                            timeoutLimit--;
                            outPak.send(host, port, sock);
                        }
                    } // end of while
                    if (timeoutLimit == 0) {
                        throw new Exception("connection failed");
                    }
                }
                System.out.println("Transfer completed.(Client " + host + ")");
                System.out.println("Filename: " + fileName + "\nSHA1 checksum: " + CheckSum.getChecksum("../" + fileName) + "\n");
            } catch (Exception e) {
                TFTPServerError ePak = new TFTPServerError(1, e.getMessage());

                try {
                    ePak.send(host, port, sock);
                } catch (Exception ignored) {
                }

                System.out.println("Client failed:  " + e.getMessage());
            }
        }
    }
}
