package ch.ethz.ssh2.packets;

/**
 * PacketGlobalForwardRequest.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: PacketGlobalForwardRequest.java,v 1.1 2005/12/05 17:13:27
 *          cplattne Exp $
 */
public class PacketGlobalForwardRequest {
    byte []        payload;

    public boolean wantReply;
    public String  bindAddress;
    public int     bindPort;

    public PacketGlobalForwardRequest (boolean wantReply, String bindAddress,
            int bindPort) {
        this.wantReply = wantReply;
        this.bindAddress = bindAddress;
        this.bindPort = bindPort;
    }

    public byte [] getPayload () {
        if (this.payload == null) {
            TypesWriter tw = new TypesWriter ();
            tw.writeByte (Packets.SSH_MSG_GLOBAL_REQUEST);

            tw.writeString ("tcpip-forward");
            tw.writeBoolean (this.wantReply);
            tw.writeString (this.bindAddress);
            tw.writeUINT32 (this.bindPort);

            this.payload = tw.getBytes ();
        }
        return this.payload;
    }
}
