package org.toilelibre.libe.remoteaudio.process.listener;

import org.toilelibre.libe.remoteaudio.process.controler.AudioBuffer;

public interface BufferActionListener {

    void onBytesRead (AudioBuffer audioBuffer);

}
