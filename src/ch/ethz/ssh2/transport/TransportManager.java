package ch.ethz.ssh2.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Vector;

import ch.ethz.ssh2.ConnectionInfo;
import ch.ethz.ssh2.ConnectionMonitor;
import ch.ethz.ssh2.DHGexParameters;
import ch.ethz.ssh2.HTTPProxyData;
import ch.ethz.ssh2.HTTPProxyException;
import ch.ethz.ssh2.ProxyData;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import ch.ethz.ssh2.crypto.Base64;
import ch.ethz.ssh2.crypto.CryptoWishList;
import ch.ethz.ssh2.crypto.cipher.BlockCipher;
import ch.ethz.ssh2.crypto.digest.MAC;
import ch.ethz.ssh2.log.Logger;
import ch.ethz.ssh2.packets.PacketDisconnect;
import ch.ethz.ssh2.packets.Packets;
import ch.ethz.ssh2.packets.TypesReader;
import ch.ethz.ssh2.util.Tokenizer;

/*
 * Yes, the "standard" is a big mess. On one side, the say that arbitary channel
 * packets are allowed during kex exchange, on the other side we need to blindly
 * ignore the next _packet_ if the KEX guess was wrong. Where do we know from
 * that the next packet is not a channel data packet? Yes, we could check if it
 * is in the KEX range. But the standard says nothing about this. The OpenSSH
 * guys block local "normal" traffic during KEX. That's fine - however, they
 * assume that the other side is doing the same. During re-key, if they receive
 * traffic other than KEX, they become horribly irritated and kill the
 * connection. Since we are very likely going to communicate with OpenSSH
 * servers, we have to play the same game - even though we could do better.
 * 
 * btw: having stdout and stderr on the same channel, with a shared window, is
 * also a VERY good idea... =(
 */

/**
 * TransportManager.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: TransportManager.java,v 1.16 2006/08/11 12:24:00 cplattne Exp $
 */
public class TransportManager {
    class AsynchronousWorker extends Thread {
        @Override
        public void run () {
            while (true) {
                byte [] msg = null;

                synchronized (TransportManager.this.asynchronousQueue) {
                    if (TransportManager.this.asynchronousQueue.size () == 0) {
                        /*
                         * After the queue is empty for about 2 seconds, stop
                         * this thread
                         */

                        try {
                            TransportManager.this.asynchronousQueue.wait (2000);
                        } catch (InterruptedException e) {
                            /*
                             * OKOK, if somebody interrupts us, then we may die
                             * earlier.
                             */
                        }

                        if (TransportManager.this.asynchronousQueue.size () == 0) {
                            TransportManager.this.asynchronousThread = null;
                            return;
                        }
                    }

                    msg = TransportManager.this.asynchronousQueue.remove (0);
                }

                /*
                 * The following invocation may throw an IOException. There is
                 * no point in handling it - it simply means that the connection
                 * has a problem and we should stop sending asynchronously
                 * messages. We do not need to signal that we have exited
                 * (asynchronousThread = null): further messages in the queue
                 * cannot be sent by this or any other thread. Other threads
                 * will sooner or later (when receiving or sending the next
                 * message) get the same IOException and get to the same
                 * conclusion.
                 */

                try {
                    TransportManager.this.sendMessage (msg);
                } catch (IOException e) {
                    return;
                }
            }
        }
    }

    class HandlerEntry {
        MessageHandler mh;
        int            low;
        int            high;
    }

    private static final Logger   log                  = Logger.getLogger (TransportManager.class);
    private final Vector<byte []> asynchronousQueue    = new Vector<byte []> ();

    private Thread                asynchronousThread   = null;

    String                        hostname;
    int                           port;
    final Socket                  sock                 = new Socket ();

    Object                        connectionSemaphore  = new Object ();

    boolean                       flagKexOngoing       = false;
    boolean                       connectionClosed     = false;

    Throwable                     reasonClosedCause    = null;

    TransportConnection           tc;
    KexManager                    km;

    Vector<HandlerEntry>          messageHandlers      = new Vector<HandlerEntry> ();

    Thread                        receiveThread;

    Vector<ConnectionMonitor>     connectionMonitors   = new Vector<ConnectionMonitor> ();
    boolean                       monitorsWereInformed = false;

    public TransportManager (String host, int port) throws IOException {
        this.hostname = host;
        this.port = port;
    }

    public void changeRecvCipher (BlockCipher bc, MAC mac) {
        this.tc.changeRecvCipher (bc, mac);
    }

    public void changeSendCipher (BlockCipher bc, MAC mac) {
        this.tc.changeSendCipher (bc, mac);
    }

    @SuppressWarnings("unchecked")
    public void close (Throwable cause, boolean useDisconnectPacket) {
        if (useDisconnectPacket == false) {
            /*
             * OK, hard shutdown - do not aquire the semaphore, perhaps somebody
             * is inside (and waits until the remote side is ready to accept new
             * data).
             */

            try {
                this.sock.close ();
            } catch (IOException ignore) {
            }

            /*
             * OK, whoever tried to send data, should now agree that there is no
             * point in further waiting =) It is safe now to aquire the
             * semaphore.
             */
        }

        synchronized (this.connectionSemaphore) {
            if (this.connectionClosed == false) {
                if (useDisconnectPacket == true) {
                    try {
                        byte [] msg = new PacketDisconnect (
                                Packets.SSH_DISCONNECT_BY_APPLICATION,
                                cause.getMessage (), "").getPayload ();
                        if (this.tc != null) {
                            this.tc.sendMessage (msg);
                        }
                    } catch (IOException ignore) {
                    }

                    try {
                        this.sock.close ();
                    } catch (IOException ignore) {
                    }
                }

                this.connectionClosed = true;
                this.reasonClosedCause = cause; /* may be null */
            }
            this.connectionSemaphore.notifyAll ();
        }

        /* No check if we need to inform the monitors */

        Vector<ConnectionMonitor> monitors = null;

        synchronized (this) {
            /*
             * Short term lock to protect "connectionMonitors" and
             * "monitorsWereInformed" (they may be modified concurrently)
             */

            if (this.monitorsWereInformed == false) {
                this.monitorsWereInformed = true;
                monitors = (Vector<ConnectionMonitor>) this.connectionMonitors
                        .clone ();
            }
        }

        if (monitors != null) {
            for (int i = 0; i < monitors.size (); i++ ) {
                try {
                    ConnectionMonitor cmon = monitors.elementAt (i);
                    cmon.connectionLost (this.reasonClosedCause);
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * There were reports that there are JDKs which use the resolver even though
     * one supplies a dotted IP address in the Socket constructor. That is why
     * we try to generate the InetAdress "by hand".
     * 
     * @param host
     * @return the InetAddress
     * @throws UnknownHostException
     */
    private InetAddress createInetAddress (String host)
            throws UnknownHostException {
        /* Check if it is a dotted IP4 address */

        InetAddress addr = this.parseIPv4Address (host);

        if (addr != null) {
            return addr;
        }

        return InetAddress.getByName (host);
    }

    private void establishConnection (ProxyData proxyData, int connectTimeout)
            throws IOException {
        /* See the comment for createInetAddress() */

        if (proxyData == null) {
            InetAddress addr = this.createInetAddress (this.hostname);
            this.sock.connect (new InetSocketAddress (addr, this.port),
                    connectTimeout);
            this.sock.setSoTimeout (0);
            return;
        }

        if (proxyData instanceof HTTPProxyData) {
            HTTPProxyData pd = (HTTPProxyData) proxyData;

            /* At the moment, we only support HTTP proxies */

            InetAddress addr = this.createInetAddress (pd.proxyHost);
            this.sock.connect (new InetSocketAddress (addr, pd.proxyPort),
                    connectTimeout);
            this.sock.setSoTimeout (0);

            /* OK, now tell the proxy where we actually want to connect to */

            StringBuffer sb = new StringBuffer ();

            sb.append ("CONNECT ");
            sb.append (this.hostname);
            sb.append (':');
            sb.append (this.port);
            sb.append (" HTTP/1.0\r\n");

            if (pd.proxyUser != null && pd.proxyPass != null) {
                String credentials = pd.proxyUser + ":" + pd.proxyPass;
                char [] encoded = Base64.encode (credentials.getBytes ());
                sb.append ("Proxy-Authorization: Basic ");
                sb.append (encoded);
                sb.append ("\r\n");
            }

            if (pd.requestHeaderLines != null) {
                for (String requestHeaderLine : pd.requestHeaderLines) {
                    if (requestHeaderLine != null) {
                        sb.append (requestHeaderLine);
                        sb.append ("\r\n");
                    }
                }
            }

            sb.append ("\r\n");

            OutputStream out = this.sock.getOutputStream ();

            out.write (sb.toString ().getBytes ());
            out.flush ();

            /* Now parse the HTTP response */

            byte [] buffer = new byte [1024];
            InputStream in = this.sock.getInputStream ();

            int len = ClientServerHello.readLineRN (in, buffer);

            String httpReponse = new String (buffer, 0, len);

            if (httpReponse.startsWith ("HTTP/") == false) {
                throw new IOException (
                        "The proxy did not send back a valid HTTP response.");
            }

            /* "HTTP/1.X XYZ X" => 14 characters minimum */

            if (httpReponse.length () < 14 || httpReponse.charAt (8) != ' '
                    || httpReponse.charAt (12) != ' ') {
                throw new IOException (
                        "The proxy did not send back a valid HTTP response.");
            }

            int errorCode = 0;

            try {
                errorCode = Integer.parseInt (httpReponse.substring (9, 12));
            } catch (NumberFormatException ignore) {
                throw new IOException (
                        "The proxy did not send back a valid HTTP response.");
            }

            if (errorCode < 0 || errorCode > 999) {
                throw new IOException (
                        "The proxy did not send back a valid HTTP response.");
            }

            if (errorCode != 200) {
                throw new HTTPProxyException (httpReponse.substring (13),
                        errorCode);
            }

            /* OK, read until empty line */

            while (true) {
                len = ClientServerHello.readLineRN (in, buffer);
                if (len == 0) {
                    break;
                }
            }
            return;
        }

        throw new IOException ("Unsupported ProxyData");
    }

    public void forceKeyExchange (CryptoWishList cwl, DHGexParameters dhgex)
            throws IOException {
        this.km.initiateKEX (cwl, dhgex);
    }

    public ConnectionInfo getConnectionInfo (int kexNumber) throws IOException {
        return this.km.getOrWaitForConnectionInfo (kexNumber);
    }

    public int getPacketOverheadEstimate () {
        return this.tc.getPacketOverheadEstimate ();
    }

    public Throwable getReasonClosedCause () {
        synchronized (this.connectionSemaphore) {
            return this.reasonClosedCause;
        }
    }

    public byte [] getSessionIdentifier () {
        return this.km.sessionId;
    }

    public void initialize (CryptoWishList cwl, ServerHostKeyVerifier verifier,
            DHGexParameters dhgex, int connectTimeout, SecureRandom rnd,
            ProxyData proxyData) throws IOException {
        /* First, establish the TCP connection to the SSH-2 server */

        this.establishConnection (proxyData, connectTimeout);

        /*
         * Parse the server line and say hello - important: this information is
         * later needed for the key exchange (to stop man-in-the-middle attacks)
         * - that is why we wrap it into an object for later use.
         */

        ClientServerHello csh = new ClientServerHello (
                this.sock.getInputStream (), this.sock.getOutputStream ());

        this.tc = new TransportConnection (this.sock.getInputStream (),
                this.sock.getOutputStream (), rnd);

        this.km = new KexManager (this, csh, cwl, this.hostname, this.port,
                verifier, rnd);
        this.km.initiateKEX (cwl, dhgex);

        this.receiveThread = new Thread (new Runnable () {
            public void run () {
                try {
                    TransportManager.this.receiveLoop ();
                } catch (IOException e) {
                    TransportManager.this.close (e, false);

                    if (TransportManager.log.isEnabled ()) {
                        TransportManager.log.log (
                                10,
                                "Receive thread: error in receiveLoop: "
                                        + e.getMessage ());
                    }
                }

                if (TransportManager.log.isEnabled ()) {
                    TransportManager.log.log (50,
                            "Receive thread: back from receiveLoop");
                }

                /* Tell all handlers that it is time to say goodbye */

                if (TransportManager.this.km != null) {
                    try {
                        TransportManager.this.km.handleMessage (null, 0);
                    } catch (IOException e) {
                    }
                }

                for (int i = 0; i < TransportManager.this.messageHandlers
                        .size (); i++ ) {
                    HandlerEntry he = TransportManager.this.messageHandlers
                            .elementAt (i);
                    try {
                        he.mh.handleMessage (null, 0);
                    } catch (Exception ignore) {
                    }
                }
            }
        });

        this.receiveThread.setDaemon (true);
        this.receiveThread.start ();
    }

    public void kexFinished () throws IOException {
        synchronized (this.connectionSemaphore) {
            this.flagKexOngoing = false;
            this.connectionSemaphore.notifyAll ();
        }
    }

    private InetAddress parseIPv4Address (String host)
            throws UnknownHostException {
        if (host == null) {
            return null;
        }

        String [] quad = Tokenizer.parseTokens (host, '.');

        if (quad == null || quad.length != 4) {
            return null;
        }

        byte [] addr = new byte [4];

        for (int i = 0; i < 4; i++ ) {
            int part = 0;

            if (quad [i].length () == 0 || quad [i].length () > 3) {
                return null;
            }

            for (int k = 0; k < quad [i].length (); k++ ) {
                char c = quad [i].charAt (k);

                /* No, Character.isDigit is not the same */
                if (c < '0' || c > '9') {
                    return null;
                }

                part = part * 10 + c - '0';
            }

            if (part > 255) {
                return null;
            }

            addr [i] = (byte) part;
        }

        return InetAddress.getByAddress (host, addr);
    }

    public void receiveLoop () throws IOException {
        byte [] msg = new byte [35000];

        while (true) {
            int msglen = this.tc.receiveMessage (msg, 0, msg.length);

            int type = msg [0] & 0xff;

            if (type == Packets.SSH_MSG_IGNORE) {
                continue;
            }

            if (type == Packets.SSH_MSG_DEBUG) {
                if (TransportManager.log.isEnabled ()) {
                    TypesReader tr = new TypesReader (msg, 0, msglen);
                    tr.readByte ();
                    tr.readBoolean ();
                    StringBuffer debugMessageBuffer = new StringBuffer ();
                    debugMessageBuffer.append (tr.readString ("UTF-8"));

                    for (int i = 0; i < debugMessageBuffer.length (); i++ ) {
                        char c = debugMessageBuffer.charAt (i);

                        if (c >= 32 && c <= 126) {
                            continue;
                        }
                        debugMessageBuffer.setCharAt (i, '\uFFFD');
                    }

                    TransportManager.log.log (
                            50,
                            "DEBUG Message from remote: '"
                                    + debugMessageBuffer.toString () + "'");
                }
                continue;
            }

            if (type == Packets.SSH_MSG_UNIMPLEMENTED) {
                throw new IOException (
                        "Peer sent UNIMPLEMENTED message, that should not happen.");
            }

            if (type == Packets.SSH_MSG_DISCONNECT) {
                TypesReader tr = new TypesReader (msg, 0, msglen);
                tr.readByte ();
                int reason_code = tr.readUINT32 ();
                StringBuffer reasonBuffer = new StringBuffer ();
                reasonBuffer.append (tr.readString ("UTF-8"));

                /*
                 * Do not get fooled by servers that send abnormal long error
                 * messages
                 */

                if (reasonBuffer.length () > 255) {
                    reasonBuffer.setLength (255);
                    reasonBuffer.setCharAt (254, '.');
                    reasonBuffer.setCharAt (253, '.');
                    reasonBuffer.setCharAt (252, '.');
                }

                /*
                 * Also, check that the server did not send charcaters that may
                 * screw up the receiver -> restrict to reasonable US-ASCII
                 * subset -> "printable characters" (ASCII 32 - 126). Replace
                 * all others with 0xFFFD (UNICODE replacement character).
                 */

                for (int i = 0; i < reasonBuffer.length (); i++ ) {
                    char c = reasonBuffer.charAt (i);

                    if (c >= 32 && c <= 126) {
                        continue;
                    }
                    reasonBuffer.setCharAt (i, '\uFFFD');
                }

                throw new IOException (
                        "Peer sent DISCONNECT message (reason code "
                                + reason_code + "): "
                                + reasonBuffer.toString ());
            }

            /*
             * Is it a KEX Packet?
             */

            if (type == Packets.SSH_MSG_KEXINIT
                    || type == Packets.SSH_MSG_NEWKEYS || type >= 30
                    && type <= 49) {
                this.km.handleMessage (msg, msglen);
                continue;
            }

            MessageHandler mh = null;

            for (int i = 0; i < this.messageHandlers.size (); i++ ) {
                HandlerEntry he = this.messageHandlers.elementAt (i);
                if (he.low <= type && type <= he.high) {
                    mh = he.mh;
                    break;
                }
            }

            if (mh == null) {
                throw new IOException ("Unexpected SSH message (type " + type
                        + ")");
            }

            mh.handleMessage (msg, msglen);
        }
    }

    public void registerMessageHandler (MessageHandler mh, int low, int high) {
        HandlerEntry he = new HandlerEntry ();
        he.mh = mh;
        he.low = low;
        he.high = high;

        synchronized (this.messageHandlers) {
            this.messageHandlers.addElement (he);
        }
    }

    public void removeMessageHandler (MessageHandler mh, int low, int high) {
        synchronized (this.messageHandlers) {
            for (int i = 0; i < this.messageHandlers.size (); i++ ) {
                HandlerEntry he = this.messageHandlers.elementAt (i);
                if (he.mh == mh && he.low == low && he.high == high) {
                    this.messageHandlers.removeElementAt (i);
                    break;
                }
            }
        }
    }

    public void sendAsynchronousMessage (byte [] msg) throws IOException {
        synchronized (this.asynchronousQueue) {
            this.asynchronousQueue.addElement (msg);

            /*
             * This limit should be flexible enough. We need this, otherwise the
             * peer can flood us with global requests (and other stuff where we
             * have to reply with an asynchronous message) and (if the server
             * just sends data and does not read what we send) this will
             * probably put us in a low memory situation (our send queue would
             * grow and grow and...)
             */

            if (this.asynchronousQueue.size () > 100) {
                throw new IOException (
                        "Error: the peer is not consuming our asynchronous replies.");
            }

            /* Check if we have an asynchronous sending thread */

            if (this.asynchronousThread == null) {
                this.asynchronousThread = new AsynchronousWorker ();
                this.asynchronousThread.setDaemon (true);
                this.asynchronousThread.start ();

                /*
                 * The thread will stop after 2 seconds of inactivity (i.e.,
                 * empty queue)
                 */
            }
        }
    }

    public void sendKexMessage (byte [] msg) throws IOException {
        synchronized (this.connectionSemaphore) {
            if (this.connectionClosed) {
                throw (IOException) new IOException (
                        "Sorry, this connection is closed.")
                        .initCause (this.reasonClosedCause);
            }

            this.flagKexOngoing = true;

            try {
                this.tc.sendMessage (msg);
            } catch (IOException e) {
                this.close (e, false);
                throw e;
            }
        }
    }

    public void sendMessage (byte [] msg) throws IOException {
        if (Thread.currentThread () == this.receiveThread) {
            throw new IOException (
                    "Assertion error: sendMessage may never be invoked by the receiver thread!");
        }

        synchronized (this.connectionSemaphore) {
            while (true) {
                if (this.connectionClosed) {
                    throw (IOException) new IOException (
                            "Sorry, this connection is closed.")
                            .initCause (this.reasonClosedCause);
                }

                if (this.flagKexOngoing == false) {
                    break;
                }

                try {
                    this.connectionSemaphore.wait ();
                } catch (InterruptedException e) {
                }
            }

            try {
                this.tc.sendMessage (msg);
            } catch (IOException e) {
                this.close (e, false);
                throw e;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void setConnectionMonitors (Vector<ConnectionMonitor> monitors) {
        synchronized (this) {
            this.connectionMonitors = (Vector<ConnectionMonitor>) monitors
                    .clone ();
        }
    }

    public void setSoTimeout (int timeout) throws IOException {
        this.sock.setSoTimeout (timeout);
    }

    public void setTcpNoDelay (boolean state) throws IOException {
        this.sock.setTcpNoDelay (state);
    }
}
