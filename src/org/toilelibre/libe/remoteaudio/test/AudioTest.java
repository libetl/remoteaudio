package org.toilelibre.libe.remoteaudio.test;

import org.toilelibre.libe.remoteaudio.audio.AndroidAudioDevice;
import org.toilelibre.libe.remoteaudio.process.controler.AudioBuffer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class AudioTest extends Activity {
    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);

        new Thread (new Runnable () {
            public void run () {
                final float frequency = 440;
                float increment = (float) (2 * Math.PI) * frequency / 44100; // angular

                int length = 1024;

                float angle = 0;
                AndroidAudioDevice device = new AndroidAudioDevice ();
                short samples[] = new short [length];

                while (true) {
                    String str = "[";
                    AudioBuffer ab = new AudioBuffer (length);
                    for (int i = 0; i < samples.length; i++ ) {
                        samples [i] = (short) (Math.sin (angle) * Short.MAX_VALUE);
                        /*
                         * str += samples [i]; if (i + 1 < samples.length){ str
                         * += ", "; }
                         */
                        angle += increment;
                    }

                    ab.setDecodedBuffer (samples);
                    ab.setActualSize (1024);
                    ab.setOffsetInBytes (0);

                    str += "]";
                    Log.v ("RemoteAudio/" + this.getClass ().getSimpleName (),
                            "Playing a sample : " + str);
                    device.addAudio (ab);
                }
            }
        }).start ();
    }
}
