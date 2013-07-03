package ch.ethz.ssh2.signature;

import java.math.BigInteger;

/**
 * DSAPrivateKey.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: DSAPrivateKey.java,v 1.1 2005/05/26 14:53:30 cplattne Exp $
 */
public class DSAPrivateKey {
    private BigInteger p;
    private BigInteger q;
    private BigInteger g;
    private BigInteger x;
    private BigInteger y;

    public DSAPrivateKey (BigInteger p, BigInteger q, BigInteger g,
            BigInteger y, BigInteger x) {
        this.p = p;
        this.q = q;
        this.g = g;
        this.y = y;
        this.x = x;
    }

    public BigInteger getG () {
        return this.g;
    }

    public BigInteger getP () {
        return this.p;
    }

    public DSAPublicKey getPublicKey () {
        return new DSAPublicKey (this.p, this.q, this.g, this.y);
    }

    public BigInteger getQ () {
        return this.q;
    }

    public BigInteger getX () {
        return this.x;
    }

    public BigInteger getY () {
        return this.y;
    }
}
