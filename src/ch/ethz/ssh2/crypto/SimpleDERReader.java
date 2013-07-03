package ch.ethz.ssh2.crypto;

import java.io.IOException;
import java.math.BigInteger;

/**
 * SimpleDERReader.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: SimpleDERReader.java,v 1.3 2006/08/11 12:24:00 cplattne Exp $
 */
public class SimpleDERReader {
    byte [] buffer;
    int     pos;
    int     count;

    public SimpleDERReader (byte [] b) {
        this.resetInput (b);
    }

    public SimpleDERReader (byte [] b, int off, int len) {
        this.resetInput (b, off, len);
    }

    public int available () {
        return this.count;
    }

    public int ignoreNextObject () throws IOException {
        int type = this.readByte () & 0xff;

        int len = this.readLength ();

        if (len < 0 || len > this.available ()) {
            throw new IOException ("Illegal len in DER object (" + len + ")");
        }

        this.readBytes (len);

        return type;
    }

    private byte readByte () throws IOException {
        if (this.count <= 0) {
            throw new IOException ("DER byte array: out of data");
        }
        this.count-- ;
        return this.buffer [this.pos++ ];
    }

    private byte [] readBytes (int len) throws IOException {
        if (len > this.count) {
            throw new IOException ("DER byte array: out of data");
        }

        byte [] b = new byte [len];

        System.arraycopy (this.buffer, this.pos, b, 0, len);

        this.pos += len;
        this.count -= len;

        return b;
    }

    public BigInteger readInt () throws IOException {
        int type = this.readByte () & 0xff;

        if (type != 0x02) {
            throw new IOException ("Expected DER Integer, but found type "
                    + type);
        }

        int len = this.readLength ();

        if (len < 0 || len > this.available ()) {
            throw new IOException ("Illegal len in DER object (" + len + ")");
        }

        byte [] b = this.readBytes (len);

        BigInteger bi = new BigInteger (b);

        return bi;
    }

    private int readLength () throws IOException {
        int len = this.readByte () & 0xff;

        if ( (len & 0x80) == 0) {
            return len;
        }

        int remain = len & 0x7F;

        if (remain == 0) {
            return -1;
        }

        len = 0;

        while (remain > 0) {
            len = len << 8;
            len = len | this.readByte () & 0xff;
            remain-- ;
        }

        return len;
    }

    public byte [] readOctetString () throws IOException {
        int type = this.readByte () & 0xff;

        if (type != 0x04) {
            throw new IOException ("Expected DER Octetstring, but found type "
                    + type);
        }

        int len = this.readLength ();

        if (len < 0 || len > this.available ()) {
            throw new IOException ("Illegal len in DER object (" + len + ")");
        }

        byte [] b = this.readBytes (len);

        return b;
    }

    public byte [] readSequenceAsByteArray () throws IOException {
        int type = this.readByte () & 0xff;

        if (type != 0x30) {
            throw new IOException ("Expected DER Sequence, but found type "
                    + type);
        }

        int len = this.readLength ();

        if (len < 0 || len > this.available ()) {
            throw new IOException ("Illegal len in DER object (" + len + ")");
        }

        byte [] b = this.readBytes (len);

        return b;
    }

    public void resetInput (byte [] b) {
        this.resetInput (b, 0, b.length);
    }

    public void resetInput (byte [] b, int off, int len) {
        this.buffer = b;
        this.pos = off;
        this.count = len;
    }

}
