package ch.ethz.ssh2.packets;

import java.io.IOException;

/**
 * PacketDisconnect.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: PacketDisconnect.java,v 1.3 2005/08/29 14:24:58 cplattne Exp $
 */
public class PacketDisconnect {
    byte [] payload;

    int     reason;
    String  desc;
    String  lang;

    public PacketDisconnect (byte payload[], int off, int len)
            throws IOException {
        this.payload = new byte [len];
        System.arraycopy (payload, off, this.payload, 0, len);

        TypesReader tr = new TypesReader (payload, off, len);

        int packet_type = tr.readByte ();

        if (packet_type != Packets.SSH_MSG_DISCONNECT) {
            throw new IOException ("This is not a Disconnect Packet! ("
                    + packet_type + ")");
        }

        this.reason = tr.readUINT32 ();
        this.desc = tr.readString ();
        this.lang = tr.readString ();
    }

    public PacketDisconnect (int reason, String desc, String lang) {
        this.reason = reason;
        this.desc = desc;
        this.lang = lang;
    }

    public byte [] getPayload () {
        if (this.payload == null) {
            TypesWriter tw = new TypesWriter ();
            tw.writeByte (Packets.SSH_MSG_DISCONNECT);
            tw.writeUINT32 (this.reason);
            tw.writeString (this.desc);
            tw.writeString (this.lang);
            this.payload = tw.getBytes ();
        }
        return this.payload;
    }
}
