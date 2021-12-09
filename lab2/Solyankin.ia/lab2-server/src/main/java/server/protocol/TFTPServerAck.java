package server.protocol;

public class TFTPServerAck extends TFTPProtocol {
    protected TFTPServerAck() {
    }

    /**
     * Generate of the Acknowledgment  packet, consisting of: opcode | Block #
     *
     * @param blockNumber - The block number of the DATA packet being acknowledged
     */
    public TFTPServerAck(int blockNumber) {
        length = 4;
        this.message = new byte[length];
        put(OPCODE_OFFSET, TFTP_ACK);
        put(BLOCK_OFFSET, (short) blockNumber);
    }

    public int blockNumber() {
        return this.get(BLOCK_OFFSET);
    }
}
