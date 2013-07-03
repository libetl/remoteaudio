package ch.ethz.ssh2.crypto.cipher;

/**
 * This is CTR mode as described in draft-ietf-secsh-newmodes-XY.txt
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: CTRMode.java,v 1.1 2005/06/06 12:44:25 cplattne Exp $
 */
public class CTRMode implements BlockCipher {
    byte []     X;
    byte []     Xenc;

    BlockCipher bc;
    int         blockSize;
    boolean     doEncrypt;

    int         count = 0;

    public CTRMode (BlockCipher tc, byte [] iv, boolean doEnc)
            throws IllegalArgumentException {
        this.bc = tc;
        this.blockSize = this.bc.getBlockSize ();
        this.doEncrypt = doEnc;

        if (this.blockSize != iv.length) {
            throw new IllegalArgumentException ("IV must be " + this.blockSize
                    + " bytes long! (currently " + iv.length + ")");
        }

        this.X = new byte [this.blockSize];
        this.Xenc = new byte [this.blockSize];

        System.arraycopy (iv, 0, this.X, 0, this.blockSize);
    }

    public final int getBlockSize () {
        return this.blockSize;
    }

    public void init (boolean forEncryption, byte [] key) {
    }

    public final void transformBlock (byte [] src, int srcoff, byte [] dst,
            int dstoff) {
        this.bc.transformBlock (this.X, 0, this.Xenc, 0);

        for (int i = 0; i < this.blockSize; i++ ) {
            dst [dstoff + i] = (byte) (src [srcoff + i] ^ this.Xenc [i]);
        }

        for (int i = this.blockSize - 1; i >= 0; i-- ) {
            this.X [i]++ ;
            if (this.X [i] != 0) {
                break;
            }

        }
    }
}
