package org.toilelibre.libe.remoteaudio.process.listener;

import org.toilelibre.libe.remoteaudio.process.controler.AudioBuffer;

public interface ProgressActionListener {

    void onBytesRead (AudioBuffer audioBuffer);

}
