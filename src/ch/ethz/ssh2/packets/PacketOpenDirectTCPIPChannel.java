package ch.ethz.ssh2.packets;

/**
 * PacketOpenDirectTCPIPChannel.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: PacketOpenDirectTCPIPChannel.java,v 1.2 2005/08/24 17:54:09
 *          cplattne Exp $
 */
public class PacketOpenDirectTCPIPChannel {
    byte [] payload;

    int     channelID;
    int     initialWindowSize;
    int     maxPacketSize;

    String  host_to_connect;
    int     port_to_connect;
    String  originator_IP_address;
    int     originator_port;

    public PacketOpenDirectTCPIPChannel (int channelID, int initialWindowSize,
            int maxPacketSize, String host_to_connect, int port_to_connect,
            String originator_IP_address, int originator_port) {
        this.channelID = channelID;
        this.initialWindowSize = initialWindowSize;
        this.maxPacketSize = maxPacketSize;
        this.host_to_connect = host_to_connect;
        this.port_to_connect = port_to_connect;
        this.originator_IP_address = originator_IP_address;
        this.originator_port = originator_port;
    }

    public byte [] getPayload () {
        if (this.payload == null) {
            TypesWriter tw = new TypesWriter ();

            tw.writeByte (Packets.SSH_MSG_CHANNEL_OPEN);
            tw.writeString ("direct-tcpip");
            tw.writeUINT32 (this.channelID);
            tw.writeUINT32 (this.initialWindowSize);
            tw.writeUINT32 (this.maxPacketSize);
            tw.writeString (this.host_to_connect);
            tw.writeUINT32 (this.port_to_connect);
            tw.writeString (this.originator_IP_address);
            tw.writeUINT32 (this.originator_port);

            this.payload = tw.getBytes ();
        }
        return this.payload;
    }
}
