package ch.ethz.ssh2.packets;

import java.io.IOException;

/**
 * PacketChannelOpenConfirmation.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: PacketChannelOpenConfirmation.java,v 1.2 2005/08/24 17:54:09
 *          cplattne Exp $
 */
public class PacketChannelOpenConfirmation {
    byte []    payload;

    public int recipientChannelID;
    public int senderChannelID;
    public int initialWindowSize;
    public int maxPacketSize;

    public PacketChannelOpenConfirmation (byte payload[], int off, int len)
            throws IOException {
        this.payload = new byte [len];
        System.arraycopy (payload, off, this.payload, 0, len);

        TypesReader tr = new TypesReader (payload, off, len);

        int packet_type = tr.readByte ();

        if (packet_type != Packets.SSH_MSG_CHANNEL_OPEN_CONFIRMATION) {
            throw new IOException (
                    "This is not a SSH_MSG_CHANNEL_OPEN_CONFIRMATION! ("
                            + packet_type + ")");
        }

        this.recipientChannelID = tr.readUINT32 ();
        this.senderChannelID = tr.readUINT32 ();
        this.initialWindowSize = tr.readUINT32 ();
        this.maxPacketSize = tr.readUINT32 ();

        if (tr.remain () != 0) {
            throw new IOException (
                    "Padding in SSH_MSG_CHANNEL_OPEN_CONFIRMATION packet!");
        }
    }

    public PacketChannelOpenConfirmation (int recipientChannelID,
            int senderChannelID, int initialWindowSize, int maxPacketSize) {
        this.recipientChannelID = recipientChannelID;
        this.senderChannelID = senderChannelID;
        this.initialWindowSize = initialWindowSize;
        this.maxPacketSize = maxPacketSize;
    }

    public byte [] getPayload () {
        if (this.payload == null) {
            TypesWriter tw = new TypesWriter ();
            tw.writeByte (Packets.SSH_MSG_CHANNEL_OPEN_CONFIRMATION);
            tw.writeUINT32 (this.recipientChannelID);
            tw.writeUINT32 (this.senderChannelID);
            tw.writeUINT32 (this.initialWindowSize);
            tw.writeUINT32 (this.maxPacketSize);
            this.payload = tw.getBytes ();
        }
        return this.payload;
    }
}
