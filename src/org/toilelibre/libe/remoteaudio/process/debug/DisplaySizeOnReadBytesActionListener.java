package org.toilelibre.libe.remoteaudio.process.debug;

import org.toilelibre.libe.remoteaudio.process.controler.AudioBuffer;
import org.toilelibre.libe.remoteaudio.process.listener.StreamActionListener;
import org.toilelibre.libe.remoteaudio.process.props.Properties;

import android.util.Log;

public class DisplaySizeOnReadBytesActionListener implements
        StreamActionListener {

    public void onBytesStream (AudioBuffer ab) {
        Log.v ("RemoteAudio/" + this.getClass ().getSimpleName (),
                "... expected " + Properties.getInstance ().getBufLength ()
                        + " bytes, " + "read " + ab.getActualSize ()
                        + " bytes ; " + "total bytes read : "
                        + (ab.getOffsetInBytes () + ab.getActualSize ())
                        + "\r");

    }

}
