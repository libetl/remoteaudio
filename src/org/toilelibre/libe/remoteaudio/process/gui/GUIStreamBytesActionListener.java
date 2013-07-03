package org.toilelibre.libe.remoteaudio.process.gui;

import org.toilelibre.libe.remoteaudio.RemoteActivity;
import org.toilelibre.libe.remoteaudio.process.controler.AudioBuffer;
import org.toilelibre.libe.remoteaudio.process.listener.StreamActionListener;

import android.os.Handler;

public class GUIStreamBytesActionListener implements StreamActionListener {

    private Handler        handler;
    private RemoteActivity src;

    public GUIStreamBytesActionListener (RemoteActivity ra, Handler h) {
        this.src = ra;
        this.handler = h;
    }

    public void onBytesSent (byte [] data, int totalSize) {
        this.handler.post (new Runnable () {

            public void run () {
                GUIStreamBytesActionListener.this.src.setPBarStreaming ();
            }

        });
    }

    public void onBytesStream (AudioBuffer audioBuffer) {

        this.handler.post (new Runnable () {

            public void run () {
                GUIStreamBytesActionListener.this.src.setPBarStreaming ();
            }

        });
    }

}
