package org.toilelibre.libe.remoteaudio.process.listener;

import org.toilelibre.libe.remoteaudio.process.controler.AudioBuffer;

public interface StreamActionListener {

    public void onBytesStream (AudioBuffer ab);
}
