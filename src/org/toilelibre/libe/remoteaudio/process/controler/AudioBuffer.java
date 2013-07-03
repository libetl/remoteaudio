package org.toilelibre.libe.remoteaudio.process.controler;

public class AudioBuffer {

    private byte []  buffer;
    private short [] decodedBuffer;
    private int      actualSize;
    private int      capacity;
    private long     offsetInBytes;

    public AudioBuffer (int bufLength) {
        super ();
        this.buffer = new byte [bufLength];
        this.actualSize = 0;
        this.capacity = bufLength;
    }

    public boolean full () {
        return this.capacity == this.actualSize;
    }

    public int getActualSize () {
        return this.actualSize;
    }

    public byte [] getBuffer () {
        return this.buffer;
    }

    public int getCapacity () {
        return this.capacity;
    }

    public short [] getDecodedBuffer () {
        return this.decodedBuffer;
    }

    public long getOffsetInBytes () {
        return this.offsetInBytes;
    }

    public void setActualSize (int actualSize1) {
        this.actualSize = actualSize1;
        byte [] newBuffer = new byte [actualSize1];
        System.arraycopy(this.buffer, 0, newBuffer, 0, actualSize1);
        this.buffer = newBuffer;
    }

    public void setBuffer (byte [] buffer1) {
        this.buffer = buffer1;
    }

    public void setCapacity (int capacity) {
        this.capacity = capacity;
    }

    public void setDecodedBuffer (short [] decodedBuffer) {
        this.decodedBuffer = decodedBuffer;
    }

    public void setOffsetInBytes (long offsetInBytes) {
        this.offsetInBytes = offsetInBytes;
    }
}
