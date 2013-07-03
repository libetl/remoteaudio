package org.toilelibre.libe.remoteaudio.arrays;

public class TransformCharShort {

    public static void byteArrayToShortArray (byte [] buffer8,
            short [] buffer16, int size) {
        int j = 0;
        for (int i = 0; i < size; i += 2) {
            j = i >>> 1;
            buffer16 [j] = (short) (buffer8 [i] & 0xff);
            if (i + 1 < buffer8.length) {
                buffer16 [j] |= (buffer8 [i + 1] & 0xff) << 8;
            }
        }
    }

    public static void shortArrayToByteArray (short [] buffer16,
            byte [] buffer8, int size) {
        int i = 0;
        for (int j = 0; j < size; j++ ) {
            buffer8 [i++ ] = (byte) (buffer16 [j] & 0xff);
            buffer8 [i++ ] = (byte) (buffer16 [j] >> 8);
        }
    }
}
