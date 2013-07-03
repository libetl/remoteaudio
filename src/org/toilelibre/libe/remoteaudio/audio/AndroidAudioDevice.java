package org.toilelibre.libe.remoteaudio.audio;

import java.util.HashMap;
import java.util.Map;

import org.toilelibre.libe.remoteaudio.process.controler.AudioBuffer;
import org.toilelibre.libe.remoteaudio.process.controler.ListenService;
import org.toilelibre.libe.remoteaudio.process.props.Properties;

import android.media.AudioManager;
import android.media.AudioTrack;

public class AndroidAudioDevice {
    private static AndroidAudioDevice instance      = null;
    private AudioTrack                track;
    private int                       minSize;
    private ListenService             listenService;
    private Object                    lock          = new Object ();
    private long                      sampleId      = 0;
    private long                      sampleSavedId = 0;
    private Map<Long, AudioBuffer>    samples;
    private boolean                   stop          = false;
    private Thread                    remoteAudioPlayer;
    private long                      delay;
    private long                      delayMin;

    public AndroidAudioDevice () {
        this (null);
    }

    public AndroidAudioDevice (ListenService ls) {
        if (AndroidAudioDevice.instance == null) {
            AndroidAudioDevice.instance = this;
            this.minSize = AudioTrack.getMinBufferSize (Properties
                    .getInstance ().getSampleRate (), Properties.getInstance ()
                    .getChannelConfig (), Properties.getInstance ()
                    .getEncoding ());
            this.track = new AudioTrack (AudioManager.STREAM_MUSIC, Properties
                    .getInstance ().getSampleRate (), Properties.getInstance ()
                    .getChannelConfig (), Properties.getInstance ()
                    .getEncoding (), this.minSize, AudioTrack.MODE_STREAM);
            this.samples = new HashMap<Long, AudioBuffer> ();
            this.listenService = ls;
            
            this.delay = 0;
            this.delayMin = Properties.getInstance ().getBufLength();
            
            this.remoteAudioPlayer = new Thread ("RemoteAudioPlayer") {
                @Override
                public void run () {
                    try {
                      Thread.sleep (5000);
                    }catch (InterruptedException ie){
                      
                    }
                    AndroidAudioDevice.this.play ();
                }
            };
        }
    }

    public void addAudio (AudioBuffer ab) {
        if (this.remoteAudioPlayer != null &&
                !this.remoteAudioPlayer.isAlive () && this.delay < this.delayMin){
            this.delay += ab.getActualSize ();
            if (this.delay >= this.delayMin){
                this.remoteAudioPlayer.start ();
            }
        }
        if (this.samples != null) {
            this.samples.put (new Long (this.sampleSavedId++ ), ab);
            synchronized (this.lock) {
                this.lock.notify ();
            }
        }
    }

    protected void play () {
        this.track.play ();
        while ( !this.stop) {
            AudioBuffer ab = this.samples.get (new Long (this.sampleId));
            while (ab == null) {
                ab = this.samples.get (new Long (this.sampleId));
                synchronized (this.lock) {
                    try {
                        this.lock.wait ();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace ();
                    }
                }
            }
            if (this.stop) {
                return;
            }
            short sample[] = ab.getDecodedBuffer ();
            this.samples.remove (new Long (this.sampleId));

            if (sample.length > 3 && sample [0] != 0) {
                AndroidAudioDevice.this.track.write (sample, 0, sample.length);
            }
            if (this.listenService != null) {
                this.listenService.stream (ab);
            }
            this.sampleId++ ;
        }
    }

    public void releaseDriver () {
        this.stop = true;
        synchronized (this.lock) {
            this.lock.notify ();
        }
        if (this.track != null) {
            this.track.stop ();
            this.track.release ();
        }
    }

}
