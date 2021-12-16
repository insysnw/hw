package client.protocol;

import java.nio.ByteBuffer;

public class Tools {
    static byte[] toBytes(int[] list) {
        int k = list.length;
        byte[] out = new byte[k];
        for (int i = 0; i < list.length; i++)
            out[i] = (byte) list[i];
        return out;
    }

    public static byte[] toByteArray(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static int unsignedByte(byte b) {
        return (int) b & 0xFF;
    }

    public static int[] unsignedBytes(byte[] Byte) {
        int[] toReturn = new int[Byte.length];
        for (int i = 0; i < Byte.length; i++)
            toReturn[i] = unsignedByte(Byte[i]);
        return toReturn;
    }

    public static void printDataBytes(byte[] data) {
        for (byte b : data)
            System.out.print(unsignedByte(b) + " ");
        System.out.println();
    }
}
