package org.toilelibre.libe.remoteaudio.audio.format;

public interface Decoder {

    public short [] decodeSamples (byte [] buffer, int size);
}
