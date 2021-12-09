package server.protocol;

public class TFTPServerError extends TFTPProtocol {
    protected TFTPServerError() {
    }

    /**
     * Generate of the Error packet, consisting of: opcode | errorCode | ErrMsh | 0
     *
     * @param errorCode - ErrorCode (see https://datatracker.ietf.org/doc/html/rfc1350#:~:text=Error%20Codes%0A%0A%20%20%20Value,No%20such%20user.)
     * @param message   - Error message
     */
    public TFTPServerError(int errorCode, String message) {
        length = 4 + message.length() + 1;
        this.message = new byte[length];
        put(OPCODE_OFFSET, TFTP_ERROR);
        put(NUM_OFFSET, (short) errorCode);
        put(MSG_OFFSET, message, (byte) 0);
    }

    public String message() {
        return this.get(MSG_OFFSET, (byte) 0);
    }
}
