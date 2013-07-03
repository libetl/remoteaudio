package ch.ethz.ssh2.packets;

import java.io.IOException;

/**
 * PacketOpenSessionChannel.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: PacketOpenSessionChannel.java,v 1.2 2005/08/24 17:54:10
 *          cplattne Exp $
 */
public class PacketOpenSessionChannel {
    byte [] payload;

    int     channelID;
    int     initialWindowSize;
    int     maxPacketSize;

    public PacketOpenSessionChannel (byte payload[], int off, int len)
            throws IOException {
        this.payload = new byte [len];
        System.arraycopy (payload, off, this.payload, 0, len);

        TypesReader tr = new TypesReader (payload);

        int packet_type = tr.readByte ();

        if (packet_type != Packets.SSH_MSG_CHANNEL_OPEN) {
            throw new IOException ("This is not a SSH_MSG_CHANNEL_OPEN! ("
                    + packet_type + ")");
        }

        this.channelID = tr.readUINT32 ();
        this.initialWindowSize = tr.readUINT32 ();
        this.maxPacketSize = tr.readUINT32 ();

        if (tr.remain () != 0) {
            throw new IOException ("Padding in SSH_MSG_CHANNEL_OPEN packet!");
        }
    }

    public PacketOpenSessionChannel (int channelID, int initialWindowSize,
            int maxPacketSize) {
        this.channelID = channelID;
        this.initialWindowSize = initialWindowSize;
        this.maxPacketSize = maxPacketSize;
    }

    public byte [] getPayload () {
        if (this.payload == null) {
            TypesWriter tw = new TypesWriter ();
            tw.writeByte (Packets.SSH_MSG_CHANNEL_OPEN);
            tw.writeString ("session");
            tw.writeUINT32 (this.channelID);
            tw.writeUINT32 (this.initialWindowSize);
            tw.writeUINT32 (this.maxPacketSize);
            this.payload = tw.getBytes ();
        }
        return this.payload;
    }
}
