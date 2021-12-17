package client.protocol;

import java.util.Arrays;

public class Options {
    private byte code;
    private byte length;
    private byte[] data;

    public Options(byte code, byte length, byte[] data) {
        this.code = code;
        this.length = length;
        this.data = data;
    }

    public byte getTotalLength() {
        return (byte) (this.length + 2);
    }

    public byte getLength() {
        return this.length;
    }

    public byte getCode() {
        return this.code;
    }

    public byte[] getData() {
        return this.data;
    }

    public byte[] getBytes() {
        int K = 2 + length;
        byte[] toSend = new byte[K];
        toSend[0] = code;
        toSend[1] = length;
        if (length >= 0) System.arraycopy(data, 0, toSend, 2, length);
        return toSend;
    }

    public String toString() {
        return "Option :" + Arrays.toString(new String[]{
                Tools.unsignedByte(code) + "",
                Tools.unsignedByte(length) + "",
                Arrays.toString(Tools.unsignedBytes(data))
        });
    }
}
