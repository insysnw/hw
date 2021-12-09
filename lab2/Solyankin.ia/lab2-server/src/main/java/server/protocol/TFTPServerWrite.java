package server.protocol;

public class TFTPServerWrite extends TFTPProtocol {
    TFTPServerWrite() {
    }

    /**
     * Generate of the Write packet, consisting of: opcode | filename | 0 | mode | 0
     *
     * @param filename - Filename
     * @param dataMode - May contain "netascii", "octet", or "mail"
     */
    public TFTPServerWrite(String filename, String dataMode) {
        length = 2 + filename.length() + 1 + dataMode.length() + 1;
        message = new byte[length];

        put(OPCODE_OFFSET, TFTP_WRQ);
        put(FILE_OFFSET, filename, (byte) 0);
        put(FILE_OFFSET + filename.length() + 1, dataMode, (byte) 0);
    }

    public String fileName() {
        return this.get(FILE_OFFSET, (byte) 0);
    }
}
