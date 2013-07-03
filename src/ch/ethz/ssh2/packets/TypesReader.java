package ch.ethz.ssh2.packets;

import java.io.IOException;
import java.math.BigInteger;

import ch.ethz.ssh2.util.Tokenizer;

/**
 * TypesReader.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: TypesReader.java,v 1.6 2006/08/31 20:04:29 cplattne Exp $
 */
public class TypesReader {
    byte [] arr;
    int     pos = 0;
    int     max = 0;

    public TypesReader (byte [] arr) {
        this.arr = arr;
        this.pos = 0;
        this.max = arr.length;
    }

    public TypesReader (byte [] arr, int off) {
        this.arr = arr;
        this.pos = off;
        this.max = arr.length;

        if (this.pos < 0 || this.pos > arr.length) {
            throw new IllegalArgumentException ("Illegal offset.");
        }
    }

    public TypesReader (byte [] arr, int off, int len) {
        this.arr = arr;
        this.pos = off;
        this.max = off + len;

        if (this.pos < 0 || this.pos > arr.length) {
            throw new IllegalArgumentException ("Illegal offset.");
        }

        if (this.max < 0 || this.max > arr.length) {
            throw new IllegalArgumentException ("Illegal length.");
        }
    }

    public boolean readBoolean () throws IOException {
        if (this.pos >= this.max) {
            throw new IOException ("Packet too short.");
        }

        return this.arr [this.pos++ ] != 0;
    }

    public int readByte () throws IOException {
        if (this.pos >= this.max) {
            throw new IOException ("Packet too short.");
        }

        return this.arr [this.pos++ ] & 0xff;
    }

    public void readBytes (byte [] dst, int off, int len) throws IOException {
        if (this.pos + len > this.max) {
            throw new IOException ("Packet too short.");
        }

        System.arraycopy (this.arr, this.pos, dst, off, len);
        this.pos += len;
    }

    public byte [] readBytes (int len) throws IOException {
        if (this.pos + len > this.max) {
            throw new IOException ("Packet too short.");
        }

        byte [] res = new byte [len];

        System.arraycopy (this.arr, this.pos, res, 0, len);
        this.pos += len;

        return res;
    }

    public byte [] readByteString () throws IOException {
        int len = this.readUINT32 ();

        if (len + this.pos > this.max) {
            throw new IOException ("Malformed SSH byte string.");
        }

        byte [] res = new byte [len];
        System.arraycopy (this.arr, this.pos, res, 0, len);
        this.pos += len;
        return res;
    }

    public BigInteger readMPINT () throws IOException {
        BigInteger b;

        byte raw[] = this.readByteString ();

        if (raw.length == 0) {
            b = BigInteger.ZERO;
        } else {
            b = new BigInteger (raw);
        }

        return b;
    }

    public String [] readNameList () throws IOException {
        return Tokenizer.parseTokens (this.readString (), ',');
    }

    public String readString () throws IOException {
        int len = this.readUINT32 ();

        if (len + this.pos > this.max) {
            throw new IOException ("Malformed SSH string.");
        }

        String res = new String (this.arr, this.pos, len);
        this.pos += len;

        return res;
    }

    public String readString (String charsetName) throws IOException {
        int len = this.readUINT32 ();

        if (len + this.pos > this.max) {
            throw new IOException ("Malformed SSH string.");
        }

        String res = charsetName == null ? new String (this.arr, this.pos, len)
                : new String (this.arr, this.pos, len, charsetName);
        this.pos += len;

        return res;
    }

    public int readUINT32 () throws IOException {
        if (this.pos + 4 > this.max) {
            throw new IOException ("Packet too short.");
        }

        return (this.arr [this.pos++ ] & 0xff) << 24
                | (this.arr [this.pos++ ] & 0xff) << 16
                | (this.arr [this.pos++ ] & 0xff) << 8 | this.arr [this.pos++ ]
                & 0xff;
    }

    public long readUINT64 () throws IOException {
        if (this.pos + 8 > this.max) {
            throw new IOException ("Packet too short.");
        }

        long high = (this.arr [this.pos++ ] & 0xff) << 24
                | (this.arr [this.pos++ ] & 0xff) << 16
                | (this.arr [this.pos++ ] & 0xff) << 8 | this.arr [this.pos++ ]
                & 0xff; /*
                         * sign extension may take place - will be shifted away
                         * =)
                         */

        long low = (this.arr [this.pos++ ] & 0xff) << 24
                | (this.arr [this.pos++ ] & 0xff) << 16
                | (this.arr [this.pos++ ] & 0xff) << 8 | this.arr [this.pos++ ]
                & 0xff; /*
                         * sign extension may take place - handle below
                         */

        return high << 32 | low & 0xffffffffl; /*
                                                * see Java language spec
                                                * (15.22.1, 5.6.2)
                                                */
    }

    public int remain () {
        return this.max - this.pos;
    }

}
