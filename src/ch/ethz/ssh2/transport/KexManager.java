package ch.ethz.ssh2.transport;

import java.io.IOException;
import java.security.SecureRandom;

import ch.ethz.ssh2.ConnectionInfo;
import ch.ethz.ssh2.DHGexParameters;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import ch.ethz.ssh2.crypto.CryptoWishList;
import ch.ethz.ssh2.crypto.KeyMaterial;
import ch.ethz.ssh2.crypto.cipher.BlockCipher;
import ch.ethz.ssh2.crypto.cipher.BlockCipherFactory;
import ch.ethz.ssh2.crypto.dh.DhExchange;
import ch.ethz.ssh2.crypto.dh.DhGroupExchange;
import ch.ethz.ssh2.crypto.digest.MAC;
import ch.ethz.ssh2.log.Logger;
import ch.ethz.ssh2.packets.PacketKexDHInit;
import ch.ethz.ssh2.packets.PacketKexDHReply;
import ch.ethz.ssh2.packets.PacketKexDhGexGroup;
import ch.ethz.ssh2.packets.PacketKexDhGexInit;
import ch.ethz.ssh2.packets.PacketKexDhGexReply;
import ch.ethz.ssh2.packets.PacketKexDhGexRequest;
import ch.ethz.ssh2.packets.PacketKexDhGexRequestOld;
import ch.ethz.ssh2.packets.PacketKexInit;
import ch.ethz.ssh2.packets.PacketNewKeys;
import ch.ethz.ssh2.packets.Packets;
import ch.ethz.ssh2.signature.DSAPublicKey;
import ch.ethz.ssh2.signature.DSASHA1Verify;
import ch.ethz.ssh2.signature.DSASignature;
import ch.ethz.ssh2.signature.RSAPublicKey;
import ch.ethz.ssh2.signature.RSASHA1Verify;
import ch.ethz.ssh2.signature.RSASignature;

/**
 * KexManager.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: KexManager.java,v 1.11 2006/09/20 12:51:37 cplattne Exp $
 */
public class KexManager {
    private static final Logger log = Logger.getLogger (KexManager.class);

    public static final void checkKexAlgorithmList (String [] algos) {
        for (String algo : algos) {
            if ("diffie-hellman-group-exchange-sha1".equals (algo)) {
                continue;
            }

            if ("diffie-hellman-group14-sha1".equals (algo)) {
                continue;
            }

            if ("diffie-hellman-group1-sha1".equals (algo)) {
                continue;
            }

            throw new IllegalArgumentException ("Unknown kex algorithm '"
                    + algo + "'");
        }
    }

    public static final void checkServerHostkeyAlgorithmsList (String [] algos) {
        for (String algo : algos) {
            if ("ssh-rsa".equals (algo) == false
                    && "ssh-dss".equals (algo) == false) {
                throw new IllegalArgumentException (
                        "Unknown server host key algorithm '" + algo + "'");
            }
        }
    }

    public static final String [] getDefaultKexAlgorithmList () {
        return new String [] { "diffie-hellman-group-exchange-sha1",
                "diffie-hellman-group14-sha1", "diffie-hellman-group1-sha1" };
    }

    public static final String [] getDefaultServerHostkeyAlgorithmList () {
        return new String [] { "ssh-rsa", "ssh-dss" };
    }

    KexState               kxs;

    int                    kexCount               = 0;
    KeyMaterial            km;

    byte []                sessionId;

    ClientServerHello      csh;

    final Object           accessLock             = new Object ();

    ConnectionInfo         lastConnInfo           = null;
    boolean                connectionClosed       = false;

    boolean                ignore_next_kex_packet = false;
    final TransportManager tm;
    CryptoWishList         nextKEXcryptoWishList;
    DHGexParameters        nextKEXdhgexParameters;

    ServerHostKeyVerifier  verifier;

    final String           hostname;

    final int              port;

    final SecureRandom     rnd;

    public KexManager (TransportManager tm, ClientServerHello csh,
            CryptoWishList initialCwl, String hostname, int port,
            ServerHostKeyVerifier keyVerifier, SecureRandom rnd) {
        this.tm = tm;
        this.csh = csh;
        this.nextKEXcryptoWishList = initialCwl;
        this.nextKEXdhgexParameters = new DHGexParameters ();
        this.hostname = hostname;
        this.port = port;
        this.verifier = keyVerifier;
        this.rnd = rnd;
    }

    private boolean compareFirstOfNameList (String [] a, String [] b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException ();
        }

        if (a.length == 0 && b.length == 0) {
            return true;
        }

        if (a.length == 0 || b.length == 0) {
            return false;
        }

        return a [0].equals (b [0]);
    }

    private boolean establishKeyMaterial () {
        try {
            int mac_cs_key_len = MAC
                    .getKeyLen (this.kxs.np.mac_algo_client_to_server);
            int enc_cs_key_len = BlockCipherFactory
                    .getKeySize (this.kxs.np.enc_algo_client_to_server);
            int enc_cs_block_len = BlockCipherFactory
                    .getBlockSize (this.kxs.np.enc_algo_client_to_server);

            int mac_sc_key_len = MAC
                    .getKeyLen (this.kxs.np.mac_algo_server_to_client);
            int enc_sc_key_len = BlockCipherFactory
                    .getKeySize (this.kxs.np.enc_algo_server_to_client);
            int enc_sc_block_len = BlockCipherFactory
                    .getBlockSize (this.kxs.np.enc_algo_server_to_client);

            this.km = KeyMaterial.create ("SHA1", this.kxs.H, this.kxs.K,
                    this.sessionId, enc_cs_key_len, enc_cs_block_len,
                    mac_cs_key_len, enc_sc_key_len, enc_sc_block_len,
                    mac_sc_key_len);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    private void finishKex () throws IOException {
        if (this.sessionId == null) {
            this.sessionId = this.kxs.H;
        }

        this.establishKeyMaterial ();

        /* Tell the other side that we start using the new material */

        PacketNewKeys ign = new PacketNewKeys ();
        this.tm.sendKexMessage (ign.getPayload ());

        BlockCipher cbc;
        MAC mac;

        try {
            cbc = BlockCipherFactory.createCipher (
                    this.kxs.np.enc_algo_client_to_server, true,
                    this.km.enc_key_client_to_server,
                    this.km.initial_iv_client_to_server);

            mac = new MAC (this.kxs.np.mac_algo_client_to_server,
                    this.km.integrity_key_client_to_server);

        } catch (IllegalArgumentException e1) {
            throw new IOException ("Fatal error during MAC startup!");
        }

        this.tm.changeSendCipher (cbc, mac);
        this.tm.kexFinished ();
    }

    private String getFirstMatch (String [] client, String [] server)
            throws NegotiateException {
        if (client == null || server == null) {
            throw new IllegalArgumentException ();
        }

        if (client.length == 0) {
            return null;
        }

        for (String element : client) {
            for (String element2 : server) {
                if (element.equals (element2)) {
                    return element;
                }
            }
        }
        throw new NegotiateException ();
    }

    public ConnectionInfo getOrWaitForConnectionInfo (int minKexCount)
            throws IOException {
        synchronized (this.accessLock) {
            while (true) {
                if (this.lastConnInfo != null
                        && this.lastConnInfo.keyExchangeCounter >= minKexCount) {
                    return this.lastConnInfo;
                }

                if (this.connectionClosed) {
                    throw (IOException) new IOException (
                            "Key exchange was not finished, connection is closed.")
                            .initCause (this.tm.getReasonClosedCause ());
                }

                try {
                    this.accessLock.wait ();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public synchronized void handleMessage (byte [] msg, int msglen)
            throws IOException {
        PacketKexInit kip;

        if (msg == null) {
            synchronized (this.accessLock) {
                this.connectionClosed = true;
                this.accessLock.notifyAll ();
                return;
            }
        }

        if (this.kxs == null && msg [0] != Packets.SSH_MSG_KEXINIT) {
            throw new IOException ("Unexpected KEX message (type " + msg [0]
                    + ")");
        }

        if (this.ignore_next_kex_packet) {
            this.ignore_next_kex_packet = false;
            return;
        }

        if (msg [0] == Packets.SSH_MSG_KEXINIT) {
            if (this.kxs != null && this.kxs.state != 0) {
                throw new IOException (
                        "Unexpected SSH_MSG_KEXINIT message during on-going kex exchange!");
            }

            if (this.kxs == null) {
                /*
                 * Ah, OK, peer wants to do KEX. Let's be nice and play
                 * together.
                 */
                this.kxs = new KexState ();
                this.kxs.dhgexParameters = this.nextKEXdhgexParameters;
                kip = new PacketKexInit (this.nextKEXcryptoWishList, this.rnd);
                this.kxs.localKEX = kip;
                this.tm.sendKexMessage (kip.getPayload ());
            }

            kip = new PacketKexInit (msg, 0, msglen);
            this.kxs.remoteKEX = kip;

            this.kxs.np = this.mergeKexParameters (
                    this.kxs.localKEX.getKexParameters (),
                    this.kxs.remoteKEX.getKexParameters ());

            if (this.kxs.np == null) {
                throw new IOException (
                        "Cannot negotiate, proposals do not match.");
            }

            if (this.kxs.remoteKEX.isFirst_kex_packet_follows ()
                    && this.kxs.np.guessOK == false) {
                /*
                 * Guess was wrong, we need to ignore the next kex packet.
                 */

                this.ignore_next_kex_packet = true;
            }

            if (this.kxs.np.kex_algo
                    .equals ("diffie-hellman-group-exchange-sha1")) {
                if (this.kxs.dhgexParameters.getMin_group_len () == 0) {
                    PacketKexDhGexRequestOld dhgexreq = new PacketKexDhGexRequestOld (
                            this.kxs.dhgexParameters);
                    this.tm.sendKexMessage (dhgexreq.getPayload ());

                } else {
                    PacketKexDhGexRequest dhgexreq = new PacketKexDhGexRequest (
                            this.kxs.dhgexParameters);
                    this.tm.sendKexMessage (dhgexreq.getPayload ());
                }
                this.kxs.state = 1;
                return;
            }

            if (this.kxs.np.kex_algo.equals ("diffie-hellman-group1-sha1")
                    || this.kxs.np.kex_algo
                            .equals ("diffie-hellman-group14-sha1")) {
                this.kxs.dhx = new DhExchange ();

                if (this.kxs.np.kex_algo.equals ("diffie-hellman-group1-sha1")) {
                    this.kxs.dhx.init (1, this.rnd);
                } else {
                    this.kxs.dhx.init (14, this.rnd);
                }

                PacketKexDHInit kp = new PacketKexDHInit (this.kxs.dhx.getE ());
                this.tm.sendKexMessage (kp.getPayload ());
                this.kxs.state = 1;
                return;
            }

            throw new IllegalStateException ("Unkown KEX method!");
        }

        if (msg [0] == Packets.SSH_MSG_NEWKEYS) {
            if (this.km == null) {
                throw new IOException (
                        "Peer sent SSH_MSG_NEWKEYS, but I have no key material ready!");
            }

            BlockCipher cbc;
            MAC mac;

            try {
                cbc = BlockCipherFactory.createCipher (
                        this.kxs.np.enc_algo_server_to_client, false,
                        this.km.enc_key_server_to_client,
                        this.km.initial_iv_server_to_client);

                mac = new MAC (this.kxs.np.mac_algo_server_to_client,
                        this.km.integrity_key_server_to_client);

            } catch (IllegalArgumentException e1) {
                throw new IOException ("Fatal error during MAC startup!");
            }

            this.tm.changeRecvCipher (cbc, mac);

            ConnectionInfo sci = new ConnectionInfo ();

            this.kexCount++ ;

            sci.keyExchangeAlgorithm = this.kxs.np.kex_algo;
            sci.keyExchangeCounter = this.kexCount;
            sci.clientToServerCryptoAlgorithm = this.kxs.np.enc_algo_client_to_server;
            sci.serverToClientCryptoAlgorithm = this.kxs.np.enc_algo_server_to_client;
            sci.clientToServerMACAlgorithm = this.kxs.np.mac_algo_client_to_server;
            sci.serverToClientMACAlgorithm = this.kxs.np.mac_algo_server_to_client;
            sci.serverHostKeyAlgorithm = this.kxs.np.server_host_key_algo;
            sci.serverHostKey = this.kxs.hostkey;

            synchronized (this.accessLock) {
                this.lastConnInfo = sci;
                this.accessLock.notifyAll ();
            }

            this.kxs = null;
            return;
        }

        if (this.kxs == null || this.kxs.state == 0) {
            throw new IOException ("Unexpected Kex submessage!");
        }

        if (this.kxs.np.kex_algo.equals ("diffie-hellman-group-exchange-sha1")) {
            if (this.kxs.state == 1) {
                PacketKexDhGexGroup dhgexgrp = new PacketKexDhGexGroup (msg, 0,
                        msglen);
                this.kxs.dhgx = new DhGroupExchange (dhgexgrp.getP (),
                        dhgexgrp.getG ());
                this.kxs.dhgx.init (this.rnd);
                PacketKexDhGexInit dhgexinit = new PacketKexDhGexInit (
                        this.kxs.dhgx.getE ());
                this.tm.sendKexMessage (dhgexinit.getPayload ());
                this.kxs.state = 2;
                return;
            }

            if (this.kxs.state == 2) {
                PacketKexDhGexReply dhgexrpl = new PacketKexDhGexReply (msg, 0,
                        msglen);

                this.kxs.hostkey = dhgexrpl.getHostKey ();

                if (this.verifier != null) {
                    boolean vres = false;

                    try {
                        vres = this.verifier.verifyServerHostKey (
                                this.hostname, this.port,
                                this.kxs.np.server_host_key_algo,
                                this.kxs.hostkey);
                    } catch (Exception e) {
                        throw (IOException) new IOException (
                                "The server hostkey was not accepted by the verifier callback.")
                                .initCause (e);
                    }

                    if (vres == false) {
                        throw new IOException (
                                "The server hostkey was not accepted by the verifier callback");
                    }
                }

                this.kxs.dhgx.setF (dhgexrpl.getF ());

                try {
                    this.kxs.H = this.kxs.dhgx.calculateH (
                            this.csh.getClientString (),
                            this.csh.getServerString (),
                            this.kxs.localKEX.getPayload (),
                            this.kxs.remoteKEX.getPayload (),
                            dhgexrpl.getHostKey (), this.kxs.dhgexParameters);
                } catch (IllegalArgumentException e) {
                    throw (IOException) new IOException ("KEX error.")
                            .initCause (e);
                }

                boolean res = this.verifySignature (dhgexrpl.getSignature (),
                        this.kxs.hostkey);

                if (res == false) {
                    throw new IOException (
                            "Hostkey signature sent by remote is wrong!");
                }

                this.kxs.K = this.kxs.dhgx.getK ();

                this.finishKex ();
                this.kxs.state = -1;
                return;
            }

            throw new IllegalStateException ("Illegal State in KEX Exchange!");
        }

        if (this.kxs.np.kex_algo.equals ("diffie-hellman-group1-sha1")
                || this.kxs.np.kex_algo.equals ("diffie-hellman-group14-sha1")) {
            if (this.kxs.state == 1) {

                PacketKexDHReply dhr = new PacketKexDHReply (msg, 0, msglen);

                this.kxs.hostkey = dhr.getHostKey ();

                if (this.verifier != null) {
                    boolean vres = false;

                    try {
                        vres = this.verifier.verifyServerHostKey (
                                this.hostname, this.port,
                                this.kxs.np.server_host_key_algo,
                                this.kxs.hostkey);
                    } catch (Exception e) {
                        throw (IOException) new IOException (
                                "The server hostkey was not accepted by the verifier callback.")
                                .initCause (e);
                    }

                    if (vres == false) {
                        throw new IOException (
                                "The server hostkey was not accepted by the verifier callback");
                    }
                }

                this.kxs.dhx.setF (dhr.getF ());

                try {
                    this.kxs.H = this.kxs.dhx
                            .calculateH (this.csh.getClientString (),
                                    this.csh.getServerString (),
                                    this.kxs.localKEX.getPayload (),
                                    this.kxs.remoteKEX.getPayload (),
                                    dhr.getHostKey ());
                } catch (IllegalArgumentException e) {
                    throw (IOException) new IOException ("KEX error.")
                            .initCause (e);
                }

                boolean res = this.verifySignature (dhr.getSignature (),
                        this.kxs.hostkey);

                if (res == false) {
                    throw new IOException (
                            "Hostkey signature sent by remote is wrong!");
                }

                this.kxs.K = this.kxs.dhx.getK ();

                this.finishKex ();
                this.kxs.state = -1;
                return;
            }
        }

        throw new IllegalStateException ("Unkown KEX method! ("
                + this.kxs.np.kex_algo + ")");
    }

    public synchronized void initiateKEX (CryptoWishList cwl,
            DHGexParameters dhgex) throws IOException {
        this.nextKEXcryptoWishList = cwl;
        this.nextKEXdhgexParameters = dhgex;

        if (this.kxs == null) {
            this.kxs = new KexState ();

            this.kxs.dhgexParameters = this.nextKEXdhgexParameters;
            PacketKexInit kp = new PacketKexInit (this.nextKEXcryptoWishList,
                    this.rnd);
            this.kxs.localKEX = kp;
            this.tm.sendKexMessage (kp.getPayload ());
        }
    }

    private boolean isGuessOK (KexParameters cpar, KexParameters spar) {
        if (cpar == null || spar == null) {
            throw new IllegalArgumentException ();
        }

        if (this.compareFirstOfNameList (cpar.kex_algorithms,
                spar.kex_algorithms) == false) {
            return false;
        }

        if (this.compareFirstOfNameList (cpar.server_host_key_algorithms,
                spar.server_host_key_algorithms) == false) {
            return false;
        }

        /*
         * We do NOT check here if the other algorithms can be agreed on, this
         * is just a check if kex_algorithms and server_host_key_algorithms were
         * guessed right!
         */

        return true;
    }

    private NegotiatedParameters mergeKexParameters (KexParameters client,
            KexParameters server) {
        NegotiatedParameters np = new NegotiatedParameters ();

        try {
            np.kex_algo = this.getFirstMatch (client.kex_algorithms,
                    server.kex_algorithms);

            KexManager.log.log (20, "kex_algo=" + np.kex_algo);

            np.server_host_key_algo = this.getFirstMatch (
                    client.server_host_key_algorithms,
                    server.server_host_key_algorithms);

            KexManager.log.log (20, "server_host_key_algo="
                    + np.server_host_key_algo);

            np.enc_algo_client_to_server = this.getFirstMatch (
                    client.encryption_algorithms_client_to_server,
                    server.encryption_algorithms_client_to_server);
            np.enc_algo_server_to_client = this.getFirstMatch (
                    client.encryption_algorithms_server_to_client,
                    server.encryption_algorithms_server_to_client);

            KexManager.log.log (20, "enc_algo_client_to_server="
                    + np.enc_algo_client_to_server);
            KexManager.log.log (20, "enc_algo_server_to_client="
                    + np.enc_algo_server_to_client);

            np.mac_algo_client_to_server = this.getFirstMatch (
                    client.mac_algorithms_client_to_server,
                    server.mac_algorithms_client_to_server);
            np.mac_algo_server_to_client = this.getFirstMatch (
                    client.mac_algorithms_server_to_client,
                    server.mac_algorithms_server_to_client);

            KexManager.log.log (20, "mac_algo_client_to_server="
                    + np.mac_algo_client_to_server);
            KexManager.log.log (20, "mac_algo_server_to_client="
                    + np.mac_algo_server_to_client);

            np.comp_algo_client_to_server = this.getFirstMatch (
                    client.compression_algorithms_client_to_server,
                    server.compression_algorithms_client_to_server);
            np.comp_algo_server_to_client = this.getFirstMatch (
                    client.compression_algorithms_server_to_client,
                    server.compression_algorithms_server_to_client);

            KexManager.log.log (20, "comp_algo_client_to_server="
                    + np.comp_algo_client_to_server);
            KexManager.log.log (20, "comp_algo_server_to_client="
                    + np.comp_algo_server_to_client);

        } catch (NegotiateException e) {
            return null;
        }

        try {
            np.lang_client_to_server = this.getFirstMatch (
                    client.languages_client_to_server,
                    server.languages_client_to_server);
        } catch (NegotiateException e1) {
            np.lang_client_to_server = null;
        }

        try {
            np.lang_server_to_client = this.getFirstMatch (
                    client.languages_server_to_client,
                    server.languages_server_to_client);
        } catch (NegotiateException e2) {
            np.lang_server_to_client = null;
        }

        if (this.isGuessOK (client, server)) {
            np.guessOK = true;
        }

        return np;
    }

    private boolean verifySignature (byte [] sig, byte [] hostkey)
            throws IOException {
        if (this.kxs.np.server_host_key_algo.equals ("ssh-rsa")) {
            RSASignature rs = RSASHA1Verify.decodeSSHRSASignature (sig);
            RSAPublicKey rpk = RSASHA1Verify.decodeSSHRSAPublicKey (hostkey);

            KexManager.log.log (50, "Verifying ssh-rsa signature");

            return RSASHA1Verify.verifySignature (this.kxs.H, rs, rpk);
        }

        if (this.kxs.np.server_host_key_algo.equals ("ssh-dss")) {
            DSASignature ds = DSASHA1Verify.decodeSSHDSASignature (sig);
            DSAPublicKey dpk = DSASHA1Verify.decodeSSHDSAPublicKey (hostkey);

            KexManager.log.log (50, "Verifying ssh-dss signature");

            return DSASHA1Verify.verifySignature (this.kxs.H, ds, dpk);
        }

        throw new IOException ("Unknown server host key algorithm '"
                + this.kxs.np.server_host_key_algo + "'");
    }
}
