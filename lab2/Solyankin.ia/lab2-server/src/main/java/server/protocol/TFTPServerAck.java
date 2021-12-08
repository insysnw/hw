package server.protocol;

public class TFTPServerAck extends TFTPProtocol {
    protected TFTPServerAck() {
    }

    //Generate ack packet
    public TFTPServerAck(int blockNumber) {
        length = 4;
        this.message = new byte[length];
        put(opOffset, tftpACK);
        put(blkOffset, (short) blockNumber);
    }

    // Accessors
    public int blockNumber() {
        return this.get(blkOffset);
    }
}
