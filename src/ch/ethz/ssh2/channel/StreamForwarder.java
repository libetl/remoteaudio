package ch.ethz.ssh2.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * A StreamForwarder forwards data between two given streams. If two
 * StreamForwarder threads are used (one for each direction) then one can be
 * configured to shutdown the underlying channel/socket if both threads have
 * finished forwarding (EOF).
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: StreamForwarder.java,v 1.2 2006/02/13 21:19:25 cplattne Exp $
 */
public class StreamForwarder extends Thread {
    OutputStream    os;
    InputStream     is;
    byte []         buffer = new byte [Channel.CHANNEL_BUFFER_SIZE];
    Channel         c;
    StreamForwarder sibling;
    Socket          s;
    String          mode;

    StreamForwarder (Channel c, StreamForwarder sibling, Socket s,
            InputStream is, OutputStream os, String mode) throws IOException {
        this.is = is;
        this.os = os;
        this.mode = mode;
        this.c = c;
        this.sibling = sibling;
        this.s = s;
    }

    @Override
    public void run () {
        try {
            while (true) {
                int len = this.is.read (this.buffer);
                if (len <= 0) {
                    break;
                }
                this.os.write (this.buffer, 0, len);
                this.os.flush ();
            }
        } catch (IOException ignore) {
            try {
                this.c.cm.closeChannel (this.c,
                        "Closed due to exception in StreamForwarder ("
                                + this.mode + "): " + ignore.getMessage (),
                        true);
            } catch (IOException e) {
            }
        } finally {
            try {
                this.os.close ();
            } catch (IOException e1) {
            }
            try {
                this.is.close ();
            } catch (IOException e2) {
            }

            if (this.sibling != null) {
                while (this.sibling.isAlive ()) {
                    try {
                        this.sibling.join ();
                    } catch (InterruptedException e) {
                    }
                }

                try {
                    this.c.cm.closeChannel (this.c, "StreamForwarder ("
                            + this.mode + ") is cleaning up the connection",
                            true);
                } catch (IOException e3) {
                }

                try {
                    if (this.s != null) {
                        this.s.close ();
                    }
                } catch (IOException e1) {
                }
            }
        }
    }
}
