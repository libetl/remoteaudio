package ch.ethz.ssh2.packets;

import java.io.IOException;

/**
 * PacketUserauthRequestPassword.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: PacketUserauthRequestNone.java,v 1.2 2005/08/24 17:54:08
 *          cplattne Exp $
 */
public class PacketUserauthRequestNone {
    byte [] payload;

    String  userName;
    String  serviceName;

    public PacketUserauthRequestNone (byte payload[], int off, int len)
            throws IOException {
        this.payload = new byte [len];
        System.arraycopy (payload, off, this.payload, 0, len);

        TypesReader tr = new TypesReader (payload, off, len);

        int packet_type = tr.readByte ();

        if (packet_type != Packets.SSH_MSG_USERAUTH_REQUEST) {
            throw new IOException ("This is not a SSH_MSG_USERAUTH_REQUEST! ("
                    + packet_type + ")");
        }

        this.userName = tr.readString ();
        this.serviceName = tr.readString ();

        String method = tr.readString ();

        if (method.equals ("none") == false) {
            throw new IOException (
                    "This is not a SSH_MSG_USERAUTH_REQUEST with type none!");
        }

        if (tr.remain () != 0) {
            throw new IOException (
                    "Padding in SSH_MSG_USERAUTH_REQUEST packet!");
        }
    }

    public PacketUserauthRequestNone (String serviceName, String user) {
        this.serviceName = serviceName;
        this.userName = user;
    }

    public byte [] getPayload () {
        if (this.payload == null) {
            TypesWriter tw = new TypesWriter ();
            tw.writeByte (Packets.SSH_MSG_USERAUTH_REQUEST);
            tw.writeString (this.userName);
            tw.writeString (this.serviceName);
            tw.writeString ("none");
            this.payload = tw.getBytes ();
        }
        return this.payload;
    }
}
