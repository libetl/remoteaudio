package org.toilelibre.libe.remoteaudio.process.end;

import org.toilelibre.libe.remoteaudio.process.driver.DriverStreamActionListener;
import org.toilelibre.libe.remoteaudio.process.listener.EndOfProcessActionListener;

public class DefaultEndOfProcessActionListener implements
        EndOfProcessActionListener {

    private DriverStreamActionListener dsal = null;

    public DefaultEndOfProcessActionListener () {
    }

    public DefaultEndOfProcessActionListener (DriverStreamActionListener d) {
        this.dsal = d;
    }

    public void onEnd (int result) {
        if (this.dsal != null) {
            this.dsal.releaseDriver ();
        }
    }

}
