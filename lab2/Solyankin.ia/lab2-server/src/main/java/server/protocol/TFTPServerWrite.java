package server.protocol;

public class TFTPServerWrite extends TFTPProtocol {
    protected TFTPServerWrite() {
    }

    public TFTPServerWrite(String filename, String dataMode) {
        length = 2 + filename.length() + 1 + dataMode.length() + 1;
        message = new byte[length];

        put(opOffset, tftpWRQ);
        put(fileOffset, filename, (byte) 0);
        put(fileOffset + filename.length() + 1, dataMode, (byte) 0);
    }

    public String fileName() {
        return this.get(fileOffset, (byte) 0);
    }

    public String requestType() {
        String name = fileName();
        return this.get(fileOffset + name.length() + 1, (byte) 0);
    }
}
