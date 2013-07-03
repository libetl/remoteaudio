package org.toilelibre.libe.remoteaudio.process.driver;

import org.toilelibre.libe.remoteaudio.audio.AndroidAudioDevice;
import org.toilelibre.libe.remoteaudio.audio.format.Decoder;
import org.toilelibre.libe.remoteaudio.process.controler.AudioBuffer;
import org.toilelibre.libe.remoteaudio.process.controler.ListenService;
import org.toilelibre.libe.remoteaudio.process.listener.BufferActionListener;
import org.toilelibre.libe.remoteaudio.process.props.Properties;
import org.toilelibre.libe.remoteaudio.process.props.SharedExecData;

public class DriverStreamActionListener implements BufferActionListener {

    private AndroidAudioDevice    aad = null;
    private Decoder               decoder;
    private ListenService         ls  = null;

    public DriverStreamActionListener () {
        this.decoder = Properties.getInstance ().getDecoder ();        
    }
    

    private void ensureInitialized () {
        if (this.aad == null) {
            if (SharedExecData.getInstance ().getResultLastExec () instanceof ListenService) {
                this.ls = (ListenService) 
                  SharedExecData.getInstance ().getResultLastExec ();
            }
            this.aad = new AndroidAudioDevice (this.ls);
        }

    }

    public void onBytesRead (AudioBuffer ab) {
        this.ensureInitialized ();

        ab.setDecodedBuffer (this.decoder.decodeSamples (ab.getBuffer (),
                ab.getActualSize ()));
        ab.setBuffer (null);

        try {
            this.aad.addAudio (ab);
        } catch (java.lang.UnsupportedOperationException uoe) {
            uoe.printStackTrace ();
        }
    }

    public void releaseDriver () {
        if (this.aad != null) {
            this.aad.releaseDriver ();
        }
    }
}
