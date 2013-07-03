package ch.ethz.ssh2.auth;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Vector;

import ch.ethz.ssh2.InteractiveCallback;
import ch.ethz.ssh2.crypto.PEMDecoder;
import ch.ethz.ssh2.packets.PacketServiceAccept;
import ch.ethz.ssh2.packets.PacketServiceRequest;
import ch.ethz.ssh2.packets.PacketUserauthBanner;
import ch.ethz.ssh2.packets.PacketUserauthFailure;
import ch.ethz.ssh2.packets.PacketUserauthInfoRequest;
import ch.ethz.ssh2.packets.PacketUserauthInfoResponse;
import ch.ethz.ssh2.packets.PacketUserauthRequestInteractive;
import ch.ethz.ssh2.packets.PacketUserauthRequestNone;
import ch.ethz.ssh2.packets.PacketUserauthRequestPassword;
import ch.ethz.ssh2.packets.PacketUserauthRequestPublicKey;
import ch.ethz.ssh2.packets.Packets;
import ch.ethz.ssh2.packets.TypesWriter;
import ch.ethz.ssh2.signature.DSAPrivateKey;
import ch.ethz.ssh2.signature.DSASHA1Verify;
import ch.ethz.ssh2.signature.DSASignature;
import ch.ethz.ssh2.signature.RSAPrivateKey;
import ch.ethz.ssh2.signature.RSASHA1Verify;
import ch.ethz.ssh2.signature.RSASignature;
import ch.ethz.ssh2.transport.MessageHandler;
import ch.ethz.ssh2.transport.TransportManager;

/**
 * AuthenticationManager.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: AuthenticationManager.java,v 1.14 2006/07/30 21:59:29 cplattne
 *          Exp $
 */
public class AuthenticationManager implements MessageHandler {
    TransportManager tm;

    Vector<byte []>  packets          = new Vector<byte []> ();
    boolean          connectionClosed = false;

    String           banner;

    String []        remainingMethods = null;
    boolean          isPartialSuccess = false;

    boolean          authenticated    = false;
    boolean          initDone         = false;

    public AuthenticationManager (TransportManager tm) {
        this.tm = tm;
    }

    public boolean authenticateInteractive (String user, String [] submethods,
            InteractiveCallback cb) throws IOException {
        try {
            this.initialize (user);

            if (this.methodPossible ("keyboard-interactive") == false) {
                throw new IOException (
                        "Authentication method keyboard-interactive not supported by the server at this stage.");
            }

            if (submethods == null) {
                submethods = new String [0];
            }

            PacketUserauthRequestInteractive ua = new PacketUserauthRequestInteractive (
                    "ssh-connection", user, submethods);

            this.tm.sendMessage (ua.getPayload ());

            while (true) {
                byte [] ar = this.getNextMessage ();

                if (ar [0] == Packets.SSH_MSG_USERAUTH_SUCCESS) {
                    this.authenticated = true;
                    this.tm.removeMessageHandler (this, 0, 255);
                    return true;
                }

                if (ar [0] == Packets.SSH_MSG_USERAUTH_FAILURE) {
                    PacketUserauthFailure puf = new PacketUserauthFailure (ar,
                            0, ar.length);

                    this.remainingMethods = puf.getAuthThatCanContinue ();
                    this.isPartialSuccess = puf.isPartialSuccess ();

                    return false;
                }

                if (ar [0] == Packets.SSH_MSG_USERAUTH_INFO_REQUEST) {
                    PacketUserauthInfoRequest pui = new PacketUserauthInfoRequest (
                            ar, 0, ar.length);

                    String [] responses;

                    try {
                        responses = cb.replyToChallenge (pui.getName (),
                                pui.getInstruction (), pui.getNumPrompts (),
                                pui.getPrompt (), pui.getEcho ());
                    } catch (Exception e) {
                        throw (IOException) new IOException (
                                "Exception in callback.").initCause (e);
                    }

                    if (responses == null) {
                        throw new IOException (
                                "Your callback may not return NULL!");
                    }

                    PacketUserauthInfoResponse puir = new PacketUserauthInfoResponse (
                            responses);
                    this.tm.sendMessage (puir.getPayload ());

                    continue;
                }

                throw new IOException ("Unexpected SSH message (type " + ar [0]
                        + ")");
            }
        } catch (IOException e) {
            this.tm.close (e, false);
            throw (IOException) new IOException (
                    "Keyboard-interactive authentication failed.")
                    .initCause (e);
        }
    }

    public boolean authenticatePassword (String user, String pass)
            throws IOException {
        try {
            this.initialize (user);

            if (this.methodPossible ("password") == false) {
                throw new IOException (
                        "Authentication method password not supported by the server at this stage.");
            }

            PacketUserauthRequestPassword ua = new PacketUserauthRequestPassword (
                    "ssh-connection", user, pass);
            this.tm.sendMessage (ua.getPayload ());

            byte [] ar = this.getNextMessage ();

            if (ar [0] == Packets.SSH_MSG_USERAUTH_SUCCESS) {
                this.authenticated = true;
                this.tm.removeMessageHandler (this, 0, 255);
                return true;
            }

            if (ar [0] == Packets.SSH_MSG_USERAUTH_FAILURE) {
                PacketUserauthFailure puf = new PacketUserauthFailure (ar, 0,
                        ar.length);

                this.remainingMethods = puf.getAuthThatCanContinue ();
                this.isPartialSuccess = puf.isPartialSuccess ();

                return false;
            }

            throw new IOException ("Unexpected SSH message (type " + ar [0]
                    + ")");

        } catch (IOException e) {
            this.tm.close (e, false);
            throw (IOException) new IOException (
                    "Password authentication failed.").initCause (e);
        }
    }

    public boolean authenticatePublicKey (String user, char [] PEMPrivateKey,
            String password, SecureRandom rnd) throws IOException {
        try {
            this.initialize (user);

            if (this.methodPossible ("publickey") == false) {
                throw new IOException (
                        "Authentication method publickey not supported by the server at this stage.");
            }

            Object key = PEMDecoder.decode (PEMPrivateKey, password);

            if (key instanceof DSAPrivateKey) {
                DSAPrivateKey pk = (DSAPrivateKey) key;

                byte [] pk_enc = DSASHA1Verify.encodeSSHDSAPublicKey (pk
                        .getPublicKey ());

                TypesWriter tw = new TypesWriter ();

                byte [] H = this.tm.getSessionIdentifier ();

                tw.writeString (H, 0, H.length);
                tw.writeByte (Packets.SSH_MSG_USERAUTH_REQUEST);
                tw.writeString (user);
                tw.writeString ("ssh-connection");
                tw.writeString ("publickey");
                tw.writeBoolean (true);
                tw.writeString ("ssh-dss");
                tw.writeString (pk_enc, 0, pk_enc.length);

                byte [] msg = tw.getBytes ();

                DSASignature ds = DSASHA1Verify
                        .generateSignature (msg, pk, rnd);

                byte [] ds_enc = DSASHA1Verify.encodeSSHDSASignature (ds);

                PacketUserauthRequestPublicKey ua = new PacketUserauthRequestPublicKey (
                        "ssh-connection", user, "ssh-dss", pk_enc, ds_enc);
                this.tm.sendMessage (ua.getPayload ());
            } else if (key instanceof RSAPrivateKey) {
                RSAPrivateKey pk = (RSAPrivateKey) key;

                byte [] pk_enc = RSASHA1Verify.encodeSSHRSAPublicKey (pk
                        .getPublicKey ());

                TypesWriter tw = new TypesWriter ();
                {
                    byte [] H = this.tm.getSessionIdentifier ();

                    tw.writeString (H, 0, H.length);
                    tw.writeByte (Packets.SSH_MSG_USERAUTH_REQUEST);
                    tw.writeString (user);
                    tw.writeString ("ssh-connection");
                    tw.writeString ("publickey");
                    tw.writeBoolean (true);
                    tw.writeString ("ssh-rsa");
                    tw.writeString (pk_enc, 0, pk_enc.length);
                }

                byte [] msg = tw.getBytes ();

                RSASignature ds = RSASHA1Verify.generateSignature (msg, pk);

                byte [] rsa_sig_enc = RSASHA1Verify.encodeSSHRSASignature (ds);

                PacketUserauthRequestPublicKey ua = new PacketUserauthRequestPublicKey (
                        "ssh-connection", user, "ssh-rsa", pk_enc, rsa_sig_enc);
                this.tm.sendMessage (ua.getPayload ());
            } else {
                throw new IOException (
                        "Unknown private key type returned by the PEM decoder.");
            }

            byte [] ar = this.getNextMessage ();

            if (ar [0] == Packets.SSH_MSG_USERAUTH_SUCCESS) {
                this.authenticated = true;
                this.tm.removeMessageHandler (this, 0, 255);
                return true;
            }

            if (ar [0] == Packets.SSH_MSG_USERAUTH_FAILURE) {
                PacketUserauthFailure puf = new PacketUserauthFailure (ar, 0,
                        ar.length);

                this.remainingMethods = puf.getAuthThatCanContinue ();
                this.isPartialSuccess = puf.isPartialSuccess ();

                return false;
            }

            throw new IOException ("Unexpected SSH message (type " + ar [0]
                    + ")");

        } catch (IOException e) {
            this.tm.close (e, false);
            throw (IOException) new IOException (
                    "Publickey authentication failed.").initCause (e);
        }
    }

    byte [] deQueue () throws IOException {
        synchronized (this.packets) {
            while (this.packets.size () == 0) {
                if (this.connectionClosed) {
                    throw (IOException) new IOException (
                            "The connection is closed.").initCause (this.tm
                            .getReasonClosedCause ());
                }

                try {
                    this.packets.wait ();
                } catch (InterruptedException ign) {
                }
            }
            /* This sequence works with J2ME */
            byte [] res = this.packets.firstElement ();
            this.packets.removeElementAt (0);
            return res;
        }
    }

    byte [] getNextMessage () throws IOException {
        while (true) {
            byte [] msg = this.deQueue ();

            if (msg [0] != Packets.SSH_MSG_USERAUTH_BANNER) {
                return msg;
            }

            PacketUserauthBanner sb = new PacketUserauthBanner (msg, 0,
                    msg.length);

            this.banner = sb.getBanner ();
        }
    }

    public boolean getPartialSuccess () {
        return this.isPartialSuccess;
    }

    public String [] getRemainingMethods (String user) throws IOException {
        this.initialize (user);
        return this.remainingMethods;
    }

    public void handleMessage (byte [] msg, int msglen) throws IOException {
        synchronized (this.packets) {
            if (msg == null) {
                this.connectionClosed = true;
            } else {
                byte [] tmp = new byte [msglen];
                System.arraycopy (msg, 0, tmp, 0, msglen);
                this.packets.addElement (tmp);
            }

            this.packets.notifyAll ();

            if (this.packets.size () > 5) {
                this.connectionClosed = true;
                throw new IOException (
                        "Error, peer is flooding us with authentication packets.");
            }
        }
    }

    private boolean initialize (String user) throws IOException {
        if (this.initDone == false) {
            this.tm.registerMessageHandler (this, 0, 255);

            PacketServiceRequest sr = new PacketServiceRequest ("ssh-userauth");
            this.tm.sendMessage (sr.getPayload ());

            PacketUserauthRequestNone urn = new PacketUserauthRequestNone (
                    "ssh-connection", user);
            this.tm.sendMessage (urn.getPayload ());

            byte [] msg = this.getNextMessage ();
            new PacketServiceAccept (msg, 0, msg.length);
            msg = this.getNextMessage ();

            this.initDone = true;

            if (msg [0] == Packets.SSH_MSG_USERAUTH_SUCCESS) {
                this.authenticated = true;
                return true;
            }

            if (msg [0] == Packets.SSH_MSG_USERAUTH_FAILURE) {
                PacketUserauthFailure puf = new PacketUserauthFailure (msg, 0,
                        msg.length);

                this.remainingMethods = puf.getAuthThatCanContinue ();
                this.isPartialSuccess = puf.isPartialSuccess ();
                return false;
            }

            throw new IOException ("Unexpected SSH message (type " + msg [0]
                    + ")");
        }
        return this.authenticated;
    }

    boolean methodPossible (String methName) {
        if (this.remainingMethods == null) {
            return false;
        }

        for (String remainingMethod : this.remainingMethods) {
            if (remainingMethod.compareTo (methName) == 0) {
                return true;
            }
        }
        return false;
    }
}
