package ch.ethz.ssh2.packets;

/**
 * PacketUserauthInfoResponse.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: PacketUserauthInfoResponse.java,v 1.3 2005/08/24 17:54:09
 *          cplattne Exp $
 */
public class PacketUserauthInfoResponse {
    byte []   payload;

    String [] responses;

    public PacketUserauthInfoResponse (String [] responses) {
        this.responses = responses;
    }

    public byte [] getPayload () {
        if (this.payload == null) {
            TypesWriter tw = new TypesWriter ();
            tw.writeByte (Packets.SSH_MSG_USERAUTH_INFO_RESPONSE);
            tw.writeUINT32 (this.responses.length);
            for (String response : this.responses) {
                tw.writeString (response);
            }

            this.payload = tw.getBytes ();
        }
        return this.payload;
    }
}
