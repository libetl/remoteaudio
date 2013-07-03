package ch.ethz.ssh2.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ch.ethz.ssh2.Connection;

/**
 * ClientServerHello.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: ClientServerHello.java,v 1.8 2006/08/02 11:57:12 cplattne Exp $
 */
public class ClientServerHello {
    public final static int readLineRN (InputStream is, byte [] buffer)
            throws IOException {
        int pos = 0;
        boolean need10 = false;
        int len = 0;
        while (true) {
            int c = is.read ();
            if (c == -1) {
                throw new IOException ("Premature connection close");
            }

            buffer [pos++ ] = (byte) c;

            if (c == 13) {
                need10 = true;
                continue;
            }

            if (c == 10) {
                break;
            }

            if (need10 == true) {
                throw new IOException (
                        "Malformed line sent by the server, the line does not end correctly.");
            }

            len++ ;
            if (pos >= buffer.length) {
                throw new IOException ("The server sent a too long line.");
            }
        }

        return len;
    }

    String server_line;

    String client_line;

    String server_versioncomment;

    public ClientServerHello (InputStream bi, OutputStream bo)
            throws IOException {
        this.client_line = "SSH-2.0-" + Connection.identification;

        bo.write ( (this.client_line + "\r\n").getBytes ());
        bo.flush ();

        byte [] serverVersion = new byte [512];

        for (int i = 0; i < 50; i++ ) {
            int len = ClientServerHello.readLineRN (bi, serverVersion);

            this.server_line = new String (serverVersion, 0, len);

            if (this.server_line.startsWith ("SSH-")) {
                break;
            }
        }

        if (this.server_line.startsWith ("SSH-") == false) {
            throw new IOException (
                    "Malformed server identification string. There was no line starting with 'SSH-' amongst the first 50 lines.");
        }

        if (this.server_line.startsWith ("SSH-1.99-")) {
            this.server_versioncomment = this.server_line.substring (9);
        } else if (this.server_line.startsWith ("SSH-2.0-")) {
            this.server_versioncomment = this.server_line.substring (8);
        } else {
            throw new IOException (
                    "Server uses incompatible protocol, it is not SSH-2 compatible.");
        }
    }

    /**
     * @return Returns the client_versioncomment.
     */
    public byte [] getClientString () {
        return this.client_line.getBytes ();
    }

    /**
     * @return Returns the server_versioncomment.
     */
    public byte [] getServerString () {
        return this.server_line.getBytes ();
    }
}
