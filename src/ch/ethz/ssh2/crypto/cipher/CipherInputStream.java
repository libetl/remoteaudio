package ch.ethz.ssh2.crypto.cipher;

import java.io.IOException;
import java.io.InputStream;

/**
 * CipherInputStream.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: CipherInputStream.java,v 1.3 2006/02/14 15:17:37 cplattne Exp $
 */
public class CipherInputStream {
    BlockCipher currentCipher;
    InputStream bi;
    byte []     buffer;
    byte []     enc;
    int         blockSize;
    int         pos;

    /*
     * We cannot use java.io.BufferedInputStream, since that is not available in
     * J2ME. Everything could be improved alot here.
     */

    final int   BUFF_SIZE         = 65536;
    byte []     input_buffer      = new byte [this.BUFF_SIZE];
    int         input_buffer_pos  = 0;
    int         input_buffer_size = 0;

    public CipherInputStream (BlockCipher tc, InputStream bi) {
        this.bi = bi;
        this.changeCipher (tc);
    }

    public void changeCipher (BlockCipher bc) {
        this.currentCipher = bc;
        this.blockSize = bc.getBlockSize ();
        this.buffer = new byte [this.blockSize];
        this.enc = new byte [this.blockSize];
        this.pos = this.blockSize;
    }

    private int fill_buffer () throws IOException {
        this.input_buffer_pos = 0;
        this.input_buffer_size = this.bi.read (this.input_buffer, 0,
                this.BUFF_SIZE);
        return this.input_buffer_size;
    }

    private void getBlock () throws IOException {
        int n = 0;
        while (n < this.blockSize) {
            int len = this.internal_read (this.enc, n, this.blockSize - n);
            if (len < 0) {
                throw new IOException ("Cannot read full block, EOF reached.");
            }
            n += len;
        }

        try {
            this.currentCipher.transformBlock (this.enc, 0, this.buffer, 0);
        } catch (Exception e) {
            throw new IOException ("Error while decrypting block.");
        }
        this.pos = 0;
    }

    private int internal_read (byte [] b, int off, int len) throws IOException {
        if (this.input_buffer_size < 0) {
            return -1;
        }

        if (this.input_buffer_pos >= this.input_buffer_size) {
            if (this.fill_buffer () <= 0) {
                return -1;
            }
        }

        int avail = this.input_buffer_size - this.input_buffer_pos;
        int thiscopy = len > avail ? avail : len;

        System.arraycopy (this.input_buffer, this.input_buffer_pos, b, off,
                thiscopy);
        this.input_buffer_pos += thiscopy;

        return thiscopy;
    }

    public int read () throws IOException {
        if (this.pos >= this.blockSize) {
            this.getBlock ();
        }
        return this.buffer [this.pos++ ] & 0xff;
    }

    public int read (byte [] dst) throws IOException {
        return this.read (dst, 0, dst.length);
    }

    public int read (byte [] dst, int off, int len) throws IOException {
        int count = 0;

        while (len > 0) {
            if (this.pos >= this.blockSize) {
                this.getBlock ();
            }

            int avail = this.blockSize - this.pos;
            int copy = Math.min (avail, len);
            System.arraycopy (this.buffer, this.pos, dst, off, copy);
            this.pos += copy;
            off += copy;
            len -= copy;
            count += copy;
        }
        return count;
    }

    public int readPlain (byte [] b, int off, int len) throws IOException {
        if (this.pos != this.blockSize) {
            throw new IOException (
                    "Cannot read plain since crypto buffer is not aligned.");
        }
        int n = 0;
        while (n < len) {
            int cnt = this.internal_read (b, off + n, len - n);
            if (cnt < 0) {
                throw new IOException ("Cannot fill buffer, EOF reached.");
            }
            n += cnt;
        }
        return n;
    }
}
