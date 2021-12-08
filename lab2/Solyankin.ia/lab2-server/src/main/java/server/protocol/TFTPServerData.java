package server.protocol;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class TFTPServerData extends TFTPProtocol {
    protected TFTPServerData() {
    }

    public TFTPServerData(int blockNumber, FileInputStream in) throws IOException {
        this.message = new byte[maxTFTPPackLen];
        // manipulate message
        this.put(opOffset, tftpDATA);
        this.put(blkOffset, (short) blockNumber);
        // read the file into packet and calculate the entire length
        length = in.read(message, dataOffset, maxTFTPData) + 4;
    }

    public int blockNumber() {
        return this.get(blkOffset);
    }

    // File output
    public int write(FileOutputStream out) throws IOException {
        out.write(message, dataOffset, length - 4);
        return (length - 4);
    }
}
