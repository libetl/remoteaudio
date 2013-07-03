package org.toilelibre.libe.remoteaudio.process.gui;

import org.toilelibre.libe.remoteaudio.RemoteActivity;
import org.toilelibre.libe.remoteaudio.process.listener.EndOfProcessActionListener;

import android.os.Handler;

public class GUIEndOfProcessActionListener implements
        EndOfProcessActionListener {

    private Handler        handler;
    private RemoteActivity src;

    public GUIEndOfProcessActionListener (RemoteActivity ra, Handler h) {
        this.src = ra;
        this.handler = h;
    }

    public void onEnd (final int result) {
        this.handler.post (new Runnable () {

            public void run () {
                GUIEndOfProcessActionListener.this.src.setPBarStop ();
            }

        });
    }
}
