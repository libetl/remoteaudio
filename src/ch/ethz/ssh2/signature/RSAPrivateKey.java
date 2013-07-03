package ch.ethz.ssh2.signature;

import java.math.BigInteger;

/**
 * RSAPrivateKey.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: RSAPrivateKey.java,v 1.1 2005/08/11 12:47:29 cplattne Exp $
 */
public class RSAPrivateKey {
    private BigInteger d;
    private BigInteger e;
    private BigInteger n;

    public RSAPrivateKey (BigInteger d, BigInteger e, BigInteger n) {
        this.d = d;
        this.e = e;
        this.n = n;
    }

    public BigInteger getD () {
        return this.d;
    }

    public BigInteger getE () {
        return this.e;
    }

    public BigInteger getN () {
        return this.n;
    }

    public RSAPublicKey getPublicKey () {
        return new RSAPublicKey (this.e, this.n);
    }
}
