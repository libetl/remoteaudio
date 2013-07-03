package ch.ethz.ssh2.channel;

import java.io.IOException;
import java.io.InputStream;

/**
 * ChannelInputStream.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: ChannelInputStream.java,v 1.5 2005/12/05 17:13:26 cplattne Exp
 *          $
 */
public final class ChannelInputStream extends InputStream {
    Channel c;

    boolean isClosed     = false;
    boolean isEOF        = false;
    boolean extendedFlag = false;

    ChannelInputStream (Channel c, boolean isExtended) {
        this.c = c;
        this.extendedFlag = isExtended;
    }

    @Override
    public int available () throws IOException {
        if (this.isEOF) {
            return 0;
        }

        int avail = this.c.cm.getAvailable (this.c, this.extendedFlag);

        /* We must not return -1 on EOF */

        return avail > 0 ? avail : 0;
    }

    @Override
    public void close () throws IOException {
        this.isClosed = true;
    }

    @Override
    public int read () throws IOException {
        /* Yes, this stream is pure and unbuffered, a single byte read() is slow */

        final byte b[] = new byte [1];

        int ret = this.read (b, 0, 1);

        if (ret != 1) {
            return -1;
        }

        return b [0] & 0xff;
    }

    @Override
    public int read (byte [] b) throws IOException {
        return this.read (b, 0, b.length);
    }

    @Override
    public int read (byte [] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException ();
        }

        if (off < 0 || len < 0 || off + len > b.length || off + len < 0
                || off > b.length) {
            throw new IndexOutOfBoundsException ();
        }

        if (len == 0) {
            return 0;
        }

        if (this.isEOF) {
            return -1;
        }

        int ret = this.c.cm.getChannelData (this.c, this.extendedFlag, b, off,
                len);

        if (ret == -1) {
            this.isEOF = true;
        }

        return ret;
    }
}
