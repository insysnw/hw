package server.protocol;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class TFTPServerData extends TFTPProtocol {
    protected TFTPServerData() {
    }

    /**
     * Generate of the Acknowledgment  packet, consisting of: opcode | Block # | data
     *
     * @param blockNumber - The block number of the DATA packet being acknowledged
     * @param data        - Data bytes
     * @throws IOException - Exception while read data bytes
     */
    public TFTPServerData(int blockNumber, FileInputStream data) throws IOException {
        this.message = new byte[TFTPPackLen];
        this.put(OPCODE_OFFSET, TFTP_DATA);
        this.put(BLOCK_OFFSET, (short) blockNumber);
        length = data.read(message, DATA_OFFSET, 512) + 4;
    }

    /**
     * File output
     *
     * @param out - FileOutputStream into which the message bytes are written
     * @return - Number of bytes written
     * @throws IOException - Exception while write bytes
     */
    public int write(FileOutputStream out) throws IOException {
        out.write(message, DATA_OFFSET, length - 4);
        return (length - 4);
    }

    public int blockNumber() {
        return this.get(BLOCK_OFFSET);
    }
}
