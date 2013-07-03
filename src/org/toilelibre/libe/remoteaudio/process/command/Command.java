package org.toilelibre.libe.remoteaudio.process.command;

public interface Command {

    public void execute ();

    public Object getResult ();

    public void setParameters (Object [] objects);

    public void stop ();
}
