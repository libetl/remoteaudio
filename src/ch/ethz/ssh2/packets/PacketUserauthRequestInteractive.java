package ch.ethz.ssh2.packets;

/**
 * PacketUserauthRequestInteractive.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: PacketUserauthRequestInteractive.java,v 1.3 2005/08/24 17:54:09
 *          cplattne Exp $
 */
public class PacketUserauthRequestInteractive {
    byte []   payload;

    String    userName;
    String    serviceName;
    String [] submethods;

    public PacketUserauthRequestInteractive (String serviceName, String user,
            String [] submethods) {
        this.serviceName = serviceName;
        this.userName = user;
        this.submethods = submethods;
    }

    public byte [] getPayload () {
        if (this.payload == null) {
            TypesWriter tw = new TypesWriter ();
            tw.writeByte (Packets.SSH_MSG_USERAUTH_REQUEST);
            tw.writeString (this.userName);
            tw.writeString (this.serviceName);
            tw.writeString ("keyboard-interactive");
            tw.writeString (""); // draft-ietf-secsh-newmodes-04.txt says that
            // the language tag should be empty.
            tw.writeNameList (this.submethods);

            this.payload = tw.getBytes ();
        }
        return this.payload;
    }
}
