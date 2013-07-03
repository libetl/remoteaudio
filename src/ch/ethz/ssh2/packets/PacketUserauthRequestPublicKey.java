package ch.ethz.ssh2.packets;

import java.io.IOException;

/**
 * PacketUserauthRequestPublicKey.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: PacketUserauthRequestPublicKey.java,v 1.2 2005/08/24 17:54:08
 *          cplattne Exp $
 */
public class PacketUserauthRequestPublicKey {
    byte [] payload;

    String  userName;
    String  serviceName;
    String  password;
    String  pkAlgoName;
    byte [] pk;
    byte [] sig;

    public PacketUserauthRequestPublicKey (byte payload[], int off, int len)
            throws IOException {
        this.payload = new byte [len];
        System.arraycopy (payload, off, this.payload, 0, len);

        TypesReader tr = new TypesReader (payload, off, len);

        int packet_type = tr.readByte ();

        if (packet_type != Packets.SSH_MSG_USERAUTH_REQUEST) {
            throw new IOException ("This is not a SSH_MSG_USERAUTH_REQUEST! ("
                    + packet_type + ")");
        }

        throw new IOException ("Not implemented!");
    }

    public PacketUserauthRequestPublicKey (String serviceName, String user,
            String pkAlgorithmName, byte [] pk, byte [] sig) {
        this.serviceName = serviceName;
        this.userName = user;
        this.pkAlgoName = pkAlgorithmName;
        this.pk = pk;
        this.sig = sig;
    }

    public byte [] getPayload () {
        if (this.payload == null) {
            TypesWriter tw = new TypesWriter ();
            tw.writeByte (Packets.SSH_MSG_USERAUTH_REQUEST);
            tw.writeString (this.userName);
            tw.writeString (this.serviceName);
            tw.writeString ("publickey");
            tw.writeBoolean (true);
            tw.writeString (this.pkAlgoName);
            tw.writeString (this.pk, 0, this.pk.length);
            tw.writeString (this.sig, 0, this.sig.length);
            this.payload = tw.getBytes ();
        }
        return this.payload;
    }
}
