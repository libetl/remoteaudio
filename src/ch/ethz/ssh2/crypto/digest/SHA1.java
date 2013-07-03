package ch.ethz.ssh2.crypto.digest;

/**
 * 
 * SHA-1 implementation based on FIPS PUB 180-1.
 * 
 * (http://www.itl.nist.gov/fipspubs/fip180-1.htm)
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: SHA1.java,v 1.4 2006/02/02 09:11:03 cplattne Exp $
 */
public final class SHA1 implements Digest {
    public static void main (String [] args) {
        SHA1 sha = new SHA1 ();

        byte [] dig1 = new byte [20];
        byte [] dig2 = new byte [20];
        byte [] dig3 = new byte [20];

        /*
         * We do not specify a charset name for getBytes(), since we assume that
         * the JVM's default encoder maps the _used_ ASCII characters exactly as
         * getBytes("US-ASCII") would do. (Ah, yes, too lazy to catch the
         * exception that can be thrown by getBytes("US-ASCII")). Note: This has
         * no effect on the SHA-1 implementation, this is just for the following
         * test code.
         */

        sha.update ("abc".getBytes ());
        sha.digest (dig1);

        sha.update ("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"
                .getBytes ());
        sha.digest (dig2);

        for (int i = 0; i < 1000000; i++ ) {
            sha.update ((byte) 'a');
        }
        sha.digest (dig3);

        String dig1_res = SHA1.toHexString (dig1);
        String dig2_res = SHA1.toHexString (dig2);
        String dig3_res = SHA1.toHexString (dig3);

        String dig1_ref = "A9993E364706816ABA3E25717850C26C9CD0D89D";
        String dig2_ref = "84983E441C3BD26EBAAE4AA1F95129E5E54670F1";
        String dig3_ref = "34AA973CD4C4DAA4F61EEB2BDBAD27316534016F";

        if (dig1_res.equals (dig1_ref)) {
            System.out.println ("SHA-1 Test 1 OK.");
        } else {
            System.out.println ("SHA-1 Test 1 FAILED.");
        }

        if (dig2_res.equals (dig2_ref)) {
            System.out.println ("SHA-1 Test 2 OK.");
        } else {
            System.out.println ("SHA-1 Test 2 FAILED.");
        }

        if (dig3_res.equals (dig3_ref)) {
            System.out.println ("SHA-1 Test 3 OK.");
        } else {
            System.out.println ("SHA-1 Test 3 FAILED.");
        }

    }

    private static final String toHexString (byte [] b) {
        final String hexChar = "0123456789ABCDEF";

        StringBuffer sb = new StringBuffer ();
        for (byte element : b) {
            sb.append (hexChar.charAt (element >> 4 & 0x0f));
            sb.append (hexChar.charAt (element & 0x0f));
        }
        return sb.toString ();
    }

    private int          H0, H1, H2, H3, H4;
    private final byte   msg[] = new byte [64];
    private final int [] w     = new int [80];

    private int          currentPos;

    private long         currentLen;

    public SHA1 () {
        this.reset ();
    }

    public final void digest (byte [] out) {
        this.digest (out, 0);
    }

    public final void digest (byte [] out, int off) {
        long l = this.currentLen;

        this.update ((byte) 0x80);

        // padding could be done more efficiently...
        while (this.currentPos != 56) {
            this.update ((byte) 0);
        }

        this.update ((byte) (l >> 56));
        this.update ((byte) (l >> 48));
        this.update ((byte) (l >> 40));
        this.update ((byte) (l >> 32));

        this.update ((byte) (l >> 24));
        this.update ((byte) (l >> 16));
        this.update ((byte) (l >> 8));
        this.update ((byte) l);

        // debug(80, H0, H1, H2, H3, H4);

        this.putInt (out, off, this.H0);
        this.putInt (out, off + 4, this.H1);
        this.putInt (out, off + 8, this.H2);
        this.putInt (out, off + 12, this.H3);
        this.putInt (out, off + 16, this.H4);

        this.reset ();
    }

    public final int getDigestLength () {
        return 20;
    }

    /*
     * private void debug(int t, int A, int B, int C, int D, int E) {
     * System.out.println(t + ": " + Integer.toHexString(A).toUpperCase() + ", "
     * + Integer.toHexString(B).toUpperCase() + ", " +
     * Integer.toHexString(C).toUpperCase() + "," +
     * Integer.toHexString(D).toUpperCase() + ", " +
     * Integer.toHexString(E).toUpperCase()); }
     */
    private final void perform () {
        for (int i = 0; i < 16; i++ ) {
            this.w [i] = (this.msg [i * 4] & 0xff) << 24
                    | (this.msg [i * 4 + 1] & 0xff) << 16
                    | (this.msg [i * 4 + 2] & 0xff) << 8 | this.msg [i * 4 + 3]
                    & 0xff;
        }

        for (int t = 16; t < 80; t++ ) {
            int x = this.w [t - 3] ^ this.w [t - 8] ^ this.w [t - 14]
                    ^ this.w [t - 16];
            this.w [t] = x << 1 | x >>> 31;
        }

        int A = this.H0;
        int B = this.H1;
        int C = this.H2;
        int D = this.H3;
        int E = this.H4;

        int T;

        for (int t = 0; t <= 19; t++ ) {
            T = (A << 5 | A >>> 27) + (B & C | ~B & D) + E + this.w [t]
                    + 0x5A827999;
            E = D;
            D = C;
            C = B << 30 | B >>> 2;
            B = A;
            A = T;
            // debug(t, A, B, C, D, E);
        }

        for (int t = 20; t <= 39; t++ ) {
            T = (A << 5 | A >>> 27) + (B ^ C ^ D) + E + this.w [t] + 0x6ED9EBA1;
            E = D;
            D = C;
            C = B << 30 | B >>> 2;
            B = A;
            A = T;
            // debug(t, A, B, C, D, E);
        }

        for (int t = 40; t <= 59; t++ ) {
            T = (A << 5 | A >>> 27) + (B & C | B & D | C & D) + E + this.w [t]
                    + 0x8F1BBCDC;
            E = D;
            D = C;
            C = B << 30 | B >>> 2;
            B = A;
            A = T;
            // debug(t, A, B, C, D, E);
        }

        for (int t = 60; t <= 79; t++ ) {
            T = (A << 5 | A >>> 27) + (B ^ C ^ D) + E + this.w [t] + 0xCA62C1D6;
            E = D;
            D = C;
            C = B << 30 | B >>> 2;
            B = A;
            A = T;
            // debug(t, A, B, C, D, E);
        }

        this.H0 = this.H0 + A;
        this.H1 = this.H1 + B;
        this.H2 = this.H2 + C;
        this.H3 = this.H3 + D;
        this.H4 = this.H4 + E;

        // debug(80, H0, H1, H2, H3, H4);
    }

    private final void putInt (byte [] b, int pos, int val) {
        b [pos] = (byte) (val >> 24);
        b [pos + 1] = (byte) (val >> 16);
        b [pos + 2] = (byte) (val >> 8);
        b [pos + 3] = (byte) val;
    }

    public final void reset () {
        this.H0 = 0x67452301;
        this.H1 = 0xEFCDAB89;
        this.H2 = 0x98BADCFE;
        this.H3 = 0x10325476;
        this.H4 = 0xC3D2E1F0;

        this.currentPos = 0;
        this.currentLen = 0;
    }

    public final void update (byte b[]) {
        for (byte element : b) {
            this.update (element);
        }
    }

    public final void update (byte b) {
        // System.out.println(pos + "->" + b);
        this.msg [this.currentPos++ ] = b;
        this.currentLen += 8;
        if (this.currentPos == 64) {
            this.perform ();
            this.currentPos = 0;
        }
    }

    public final void update (byte b[], int off, int len) {
        for (int i = off; i < off + len; i++ ) {
            this.update (b [i]);
        }
    }
}
