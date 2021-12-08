package server.requests;

import server.protocol.*;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class TFTPServerWRQ extends Thread {

    private DatagramSocket sock;
    private InetAddress host;
    private int port;
    private FileOutputStream outFile;
    private TFTPProtocol req;
    private int timeoutLimit = 5;
    private File saveFile;
    private String fileName;

    public TFTPServerWRQ(TFTPServerWrite request) {
        try {
            req = request;
            sock = new DatagramSocket(); // new port for transfer
            sock.setSoTimeout(1000);

            host = request.getAddress();
            port = request.getPort();
            fileName = request.fileName();
            //create file object in parent folder
            saveFile = new File(System.getProperty("user.home") + File.separator +
                    "Desktop" + File.separator + fileName);

            if (!saveFile.exists()) {
                outFile = new FileOutputStream(saveFile);
                TFTPServerAck a = new TFTPServerAck(0);
                a.send(host, port, sock); // send ack 0 at first, ready to
                // receive
                this.start();
            } else
                throw new TFTPServerException("access violation, file exists");

        } catch (Exception e) {
            TFTPServerError ePak = new TFTPServerError(1, e.getMessage()); // error code 1
            try {
                ePak.send(host, port, sock);
            } catch (Exception ignored) {
            }

            System.out.println("Client start failed:" + e.getMessage());
        }
    }

    public void run() {
        // handle write request
        if (req instanceof TFTPServerWrite) {
            try {
                for (int blkNum = 1, bytesOut = 512; bytesOut == 512; blkNum++) {
                    while (timeoutLimit != 0) {
                        try {
                            TFTPProtocol inPak = TFTPProtocol.receive(sock);
                            //check packet type
                            if (inPak instanceof TFTPServerError) {
                                TFTPServerError p = (TFTPServerError) inPak;
                                throw new TFTPServerException(p.message());
                            } else if (inPak instanceof TFTPServerData) {
                                TFTPServerData p = (TFTPServerData) inPak;
                                /*System.out.println("incoming data " + p.blockNumber());*/
                                // check blk num
                                if (/*testloss==20||*/p.blockNumber() != blkNum) { //expect to be the same
                                    //System.out.println("loss. testloss="+testloss+"timeoutLimit="+timeoutLimit);
                                    //testloss++;
                                    throw new SocketTimeoutException();
                                }
                                //write to the file and send ack
                                bytesOut = p.write(outFile);
                                TFTPServerAck a = new TFTPServerAck(blkNum);
                                a.send(host, port, sock);
                                //testloss++;
                                break;
                            }
                        } catch (SocketTimeoutException t2) {
                            System.out.println("Time out, resend ack");
                            TFTPServerAck a = new TFTPServerAck(blkNum - 1);
                            a.send(host, port, sock);
                            timeoutLimit--;
                        }
                    }
                    if (timeoutLimit == 0) {
                        throw new Exception("Connection failed");
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
                saveFile.delete();
            }
        }
    }
}
