package ch.ethz.ssh2.packets;

/**
 * PacketSessionX11Request.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: PacketSessionX11Request.java,v 1.2 2005/12/05 17:13:27 cplattne
 *          Exp $
 */
public class PacketSessionX11Request {
    byte []        payload;

    public int     recipientChannelID;
    public boolean wantReply;

    public boolean singleConnection;
    String         x11AuthenticationProtocol;
    String         x11AuthenticationCookie;
    int            x11ScreenNumber;

    public PacketSessionX11Request (int recipientChannelID, boolean wantReply,
            boolean singleConnection, String x11AuthenticationProtocol,
            String x11AuthenticationCookie, int x11ScreenNumber) {
        this.recipientChannelID = recipientChannelID;
        this.wantReply = wantReply;

        this.singleConnection = singleConnection;
        this.x11AuthenticationProtocol = x11AuthenticationProtocol;
        this.x11AuthenticationCookie = x11AuthenticationCookie;
        this.x11ScreenNumber = x11ScreenNumber;
    }

    public byte [] getPayload () {
        if (this.payload == null) {
            TypesWriter tw = new TypesWriter ();
            tw.writeByte (Packets.SSH_MSG_CHANNEL_REQUEST);
            tw.writeUINT32 (this.recipientChannelID);
            tw.writeString ("x11-req");
            tw.writeBoolean (this.wantReply);

            tw.writeBoolean (this.singleConnection);
            tw.writeString (this.x11AuthenticationProtocol);
            tw.writeString (this.x11AuthenticationCookie);
            tw.writeUINT32 (this.x11ScreenNumber);

            this.payload = tw.getBytes ();
        }
        return this.payload;
    }
}
