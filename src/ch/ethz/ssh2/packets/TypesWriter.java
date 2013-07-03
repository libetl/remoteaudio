package ch.ethz.ssh2.packets;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

/**
 * TypesWriter.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: TypesWriter.java,v 1.6 2006/08/31 20:04:29 cplattne Exp $
 */
public class TypesWriter {
    byte arr[];
    int  pos;

    public TypesWriter () {
        this.arr = new byte [256];
        this.pos = 0;
    }

    public byte [] getBytes () {
        byte [] dst = new byte [this.pos];
        System.arraycopy (this.arr, 0, dst, 0, this.pos);
        return dst;
    }

    public void getBytes (byte dst[]) {
        System.arraycopy (this.arr, 0, dst, 0, this.pos);
    }

    public int length () {
        return this.pos;
    }

    private void resize (int len) {
        byte new_arr[] = new byte [len];
        System.arraycopy (this.arr, 0, new_arr, 0, this.arr.length);
        this.arr = new_arr;
    }

    public void writeBoolean (boolean v) {
        if (this.pos + 1 > this.arr.length) {
            this.resize (this.arr.length + 32);
        }

        this.arr [this.pos++ ] = v ? (byte) 1 : (byte) 0;
    }

    public void writeByte (int v) {
        this.writeByte (v, this.pos);
        this.pos++ ;
    }

    public void writeByte (int v, int off) {
        if (off + 1 > this.arr.length) {
            this.resize (off + 32);
        }

        this.arr [off] = (byte) v;
    }

    public void writeBytes (byte [] buff) {
        this.writeBytes (buff, 0, buff.length);
    }

    public void writeBytes (byte [] buff, int off, int len) {
        if (this.pos + len > this.arr.length) {
            this.resize (this.arr.length + len + 32);
        }

        System.arraycopy (buff, off, this.arr, this.pos, len);
        this.pos += len;
    }

    public void writeMPInt (BigInteger b) {
        byte raw[] = b.toByteArray ();

        if (raw.length == 1 && raw [0] == 0) {
            this.writeUINT32 (0); /* String with zero bytes of data */
        } else {
            this.writeString (raw, 0, raw.length);
        }
    }

    public void writeNameList (String v[]) {
        StringBuffer sb = new StringBuffer ();
        for (int i = 0; i < v.length; i++ ) {
            if (i > 0) {
                sb.append (',');
            }
            sb.append (v [i]);
        }
        this.writeString (sb.toString ());
    }

    public void writeString (byte [] buff, int off, int len) {
        this.writeUINT32 (len);
        this.writeBytes (buff, off, len);
    }

    public void writeString (String v) {
        byte [] b = v.getBytes ();

        this.writeUINT32 (b.length);
        this.writeBytes (b, 0, b.length);
    }

    public void writeString (String v, String charsetName)
            throws UnsupportedEncodingException {
        byte [] b = charsetName == null ? v.getBytes () : v
                .getBytes (charsetName);

        this.writeUINT32 (b.length);
        this.writeBytes (b, 0, b.length);
    }

    public void writeUINT32 (int val) {
        this.writeUINT32 (val, this.pos);
        this.pos += 4;
    }

    public void writeUINT32 (int val, int off) {
        if (off + 4 > this.arr.length) {
            this.resize (off + 32);
        }

        this.arr [off++ ] = (byte) (val >> 24);
        this.arr [off++ ] = (byte) (val >> 16);
        this.arr [off++ ] = (byte) (val >> 8);
        this.arr [off++ ] = (byte) val;
    }

    public void writeUINT64 (long val) {
        if (this.pos + 8 > this.arr.length) {
            this.resize (this.arr.length + 32);
        }

        this.arr [this.pos++ ] = (byte) (val >> 56);
        this.arr [this.pos++ ] = (byte) (val >> 48);
        this.arr [this.pos++ ] = (byte) (val >> 40);
        this.arr [this.pos++ ] = (byte) (val >> 32);
        this.arr [this.pos++ ] = (byte) (val >> 24);
        this.arr [this.pos++ ] = (byte) (val >> 16);
        this.arr [this.pos++ ] = (byte) (val >> 8);
        this.arr [this.pos++ ] = (byte) val;
    }
}
