package org.toilelibre.libe.remoteaudio.process.debug;

import org.toilelibre.libe.remoteaudio.process.controler.AudioBuffer;
import org.toilelibre.libe.remoteaudio.process.listener.BufferActionListener;

public class SysOutStreamActionListener implements BufferActionListener {

    public void onBytesRead (AudioBuffer audioBuffer) {
        System.out.println (new String (audioBuffer.getBuffer (), 0,
                audioBuffer.getActualSize ()));

    }

}
