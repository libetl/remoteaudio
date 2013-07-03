package ch.ethz.ssh2.crypto.dh;

import java.math.BigInteger;
import java.security.SecureRandom;

import ch.ethz.ssh2.DHGexParameters;
import ch.ethz.ssh2.crypto.digest.HashForSSH2Types;

/**
 * DhGroupExchange.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: DhGroupExchange.java,v 1.6 2006/09/20 12:51:37 cplattne Exp $
 */
public class DhGroupExchange {
    /* Given by the standard */

    private BigInteger p;
    private BigInteger g;

    /* Client public and private */

    private BigInteger e;
    private BigInteger x;

    /* Server public */

    private BigInteger f;

    /* Shared secret */

    private BigInteger k;

    public DhGroupExchange (BigInteger p, BigInteger g) {
        this.p = p;
        this.g = g;
    }

    public byte [] calculateH (byte [] clientversion, byte [] serverversion,
            byte [] clientKexPayload, byte [] serverKexPayload,
            byte [] hostKey, DHGexParameters para) {
        HashForSSH2Types hash = new HashForSSH2Types ("SHA1");

        hash.updateByteString (clientversion);
        hash.updateByteString (serverversion);
        hash.updateByteString (clientKexPayload);
        hash.updateByteString (serverKexPayload);
        hash.updateByteString (hostKey);
        if (para.getMin_group_len () > 0) {
            hash.updateUINT32 (para.getMin_group_len ());
        }
        hash.updateUINT32 (para.getPref_group_len ());
        if (para.getMax_group_len () > 0) {
            hash.updateUINT32 (para.getMax_group_len ());
        }
        hash.updateBigInt (this.p);
        hash.updateBigInt (this.g);
        hash.updateBigInt (this.e);
        hash.updateBigInt (this.f);
        hash.updateBigInt (this.k);

        return hash.getDigest ();
    }

    /**
     * @return Returns the e.
     */
    public BigInteger getE () {
        if (this.e == null) {
            throw new IllegalStateException ("Not initialized!");
        }

        return this.e;
    }

    /**
     * @return Returns the shared secret k.
     */
    public BigInteger getK () {
        if (this.k == null) {
            throw new IllegalStateException (
                    "Shared secret not yet known, need f first!");
        }

        return this.k;
    }

    public void init (SecureRandom rnd) {
        this.k = null;

        this.x = new BigInteger (this.p.bitLength () - 1, rnd);
        this.e = this.g.modPow (this.x, this.p);
    }

    /**
     * Sets f and calculates the shared secret.
     */
    public void setF (BigInteger f) {
        if (this.e == null) {
            throw new IllegalStateException ("Not initialized!");
        }

        BigInteger zero = BigInteger.valueOf (0);

        if (zero.compareTo (f) >= 0 || this.p.compareTo (f) <= 0) {
            throw new IllegalArgumentException ("Invalid f specified!");
        }

        this.f = f;
        this.k = f.modPow (this.x, this.p);
    }
}
