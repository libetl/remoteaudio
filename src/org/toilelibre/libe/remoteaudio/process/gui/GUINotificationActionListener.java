package org.toilelibre.libe.remoteaudio.process.gui;

import org.toilelibre.libe.remoteaudio.RemoteActivity;
import org.toilelibre.libe.remoteaudio.process.controler.AudioBuffer;
import org.toilelibre.libe.remoteaudio.process.listener.EndOfProcessActionListener;
import org.toilelibre.libe.remoteaudio.process.listener.StreamActionListener;

import android.os.Handler;

public class GUINotificationActionListener implements StreamActionListener,
        EndOfProcessActionListener {

    private Handler        handler;
    private RemoteActivity src;

    public GUINotificationActionListener (RemoteActivity ra, Handler h) {
        this.src = ra;
        this.handler = h;
    }

    public void onBytesStream (AudioBuffer audioBuffer) {
        this.handler.post (new Runnable () {

            public void run () {
                GUINotificationActionListener.this.src.createNotification ();
            }

        });

    }

    public void onEnd (int result) {
        GUINotificationActionListener.this.src.dismissNotification ();
    }

}
