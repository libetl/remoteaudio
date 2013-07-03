package org.toilelibre.libe.remoteaudio.process.gui;

import org.toilelibre.libe.remoteaudio.RemoteActivity;
import org.toilelibre.libe.remoteaudio.process.controler.AudioBuffer;
import org.toilelibre.libe.remoteaudio.process.listener.BufferActionListener;
import org.toilelibre.libe.remoteaudio.process.props.Properties;

import android.os.Handler;

public class GUIReadBytesActionListener implements BufferActionListener {

    private Handler        handler;
    private RemoteActivity src;

    public GUIReadBytesActionListener (RemoteActivity ra, Handler h) {
        this.src = ra;
        this.handler = h;
    }

    public void onBytesRead (AudioBuffer ab) {
        GUIReadBytesActionListener.this.src.setPBarReading (
                ab.getActualSize (), Properties.getInstance ().getBufLength ());

    }

    public void onBytesStream (byte [] data, final int totalSize,
            long lastReadSize) {
        this.handler.post (new Runnable () {

            public void run () {
                GUIReadBytesActionListener.this.src.setPBarReading (totalSize,
                        Properties.getInstance ().getBufLength ());
            }

        });
    }

}
