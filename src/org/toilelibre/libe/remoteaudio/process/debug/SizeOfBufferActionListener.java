package org.toilelibre.libe.remoteaudio.process.debug;

import org.toilelibre.libe.remoteaudio.process.controler.AudioBuffer;
import org.toilelibre.libe.remoteaudio.process.listener.BufferActionListener;

import android.util.Log;

public class SizeOfBufferActionListener implements BufferActionListener {

    public void onBytesRead (AudioBuffer ab) {
        Log.v ("RemoteAudio/" + this.getClass ().getSimpleName (),
                "new frame of " + ab.getActualSize () + " bytes sent");

    }
}
