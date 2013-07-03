package org.toilelibre.libe.remoteaudio.process.command;

import java.io.IOException;
import java.util.List;

import org.toilelibre.libe.remoteaudio.process.controler.ListenUnixCommandService;
import org.toilelibre.libe.remoteaudio.process.listener.BufferActionListener;
import org.toilelibre.libe.remoteaudio.process.listener.EndOfProcessActionListener;
import org.toilelibre.libe.remoteaudio.process.listener.StreamActionListener;

import android.util.Log;

public class RunUnixCommand implements Command {

    private StringBuffer                     prefix = new StringBuffer ("");

    private List<BufferActionListener>       bals;

    private List<EndOfProcessActionListener> eopls;

    private List<StreamActionListener>       sals;

    private ListenUnixCommandService          lt;

    private Process                          process;

    public void execute () {
        Runtime r = Runtime.getRuntime ();
        try {
            Log.v ("RemoteAudio/" + this.getClass ().getSimpleName (),
                    this.prefix.toString ());
            this.process = r.exec (this.prefix.toString ());
            this.lt = new ListenUnixCommandService (this.process, this.bals,
                    this.sals, this.eopls);
            this.lt.start ();
        } catch (IOException e) {
            e.printStackTrace ();
        }
    }

    public Object getResult () {
        return this.lt;
    }

    @SuppressWarnings("unchecked")
    public void setParameters (Object [] objects) {
        if (objects.length >= 4) {
            if (objects [0] instanceof StringBuffer) {
                this.prefix = (StringBuffer) objects [0];
            }
            if (objects [1] instanceof List) {
                this.bals = (List<BufferActionListener>) objects [1];
            }
            if (objects [2] instanceof List) {
                this.sals = (List<StreamActionListener>) objects [2];
            }
            if (objects [3] instanceof List) {
                this.eopls = (List<EndOfProcessActionListener>) objects [3];
            }
        }
    }

    public void stop () {
        if (this.lt != null) {
            this.lt.setStop (true);
        }
    }
}
