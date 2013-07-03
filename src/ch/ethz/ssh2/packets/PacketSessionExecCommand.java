package ch.ethz.ssh2.packets;

/**
 * PacketSessionExecCommand.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: PacketSessionExecCommand.java,v 1.2 2005/08/24 17:54:09
 *          cplattne Exp $
 */
public class PacketSessionExecCommand {
    byte []        payload;

    public int     recipientChannelID;
    public boolean wantReply;
    public String  command;

    public PacketSessionExecCommand (int recipientChannelID, boolean wantReply,
            String command) {
        this.recipientChannelID = recipientChannelID;
        this.wantReply = wantReply;
        this.command = command;
    }

    public byte [] getPayload () {
        if (this.payload == null) {
            TypesWriter tw = new TypesWriter ();
            tw.writeByte (Packets.SSH_MSG_CHANNEL_REQUEST);
            tw.writeUINT32 (this.recipientChannelID);
            tw.writeString ("exec");
            tw.writeBoolean (this.wantReply);
            tw.writeString (this.command);
            this.payload = tw.getBytes ();
        }
        return this.payload;
    }
}
