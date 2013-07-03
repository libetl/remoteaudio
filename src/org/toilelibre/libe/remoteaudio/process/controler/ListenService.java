package org.toilelibre.libe.remoteaudio.process.controler;

public interface ListenService {

    public int getResult ();

    public void run ();

    public void setStop (boolean stop);

    public void stream (AudioBuffer ab);

}
