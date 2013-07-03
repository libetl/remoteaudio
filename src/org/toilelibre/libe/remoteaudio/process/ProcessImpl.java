package org.toilelibre.libe.remoteaudio.process;


import org.toilelibre.libe.remoteaudio.process.command.Command;
import org.toilelibre.libe.remoteaudio.process.props.Properties;
import org.toilelibre.libe.remoteaudio.process.props.SharedExecData;

import android.util.Log;

public class ProcessImpl {

    private ProcessConfiguration config;
    private Command [] commands;

    public ProcessImpl () {
        super ();
    }

    public ProcessImpl (ProcessConfiguration pc) {
        this.config = pc;
    }

    public void setConfig (ProcessConfiguration config) {
        this.config = config;
    }

    public void run () {

        if (this.config == null){
            return;
        }
        
        Object p = new StringBuffer ("");

        this.commands = Properties.getInstance ().getCommandsByType (
                Properties.getInstance ().getActiveCommandsSet ());

        Object [][] parameters = new Object [this.commands.length] [];

        for (int i = 0; i < this.commands.length - 1; i++ ) {
            parameters [i] = new Object [] { p };
        }

        parameters [this.commands.length - 1] = new Object [] { p, this.config.getBals (),
                this.config.getSals (), this.config.getEopls () };

        for (int i = 0; i < this.commands.length; i++ ) {
            Command c = this.commands [i];
            c.setParameters (parameters [i]);
            c.execute ();
            p = c.getResult ();
        }

        SharedExecData.getInstance ().setResultLastExec (p);
        
        Log.v ("RemoteAudio/" + this.getClass ().getSimpleName (),
                "Result Object : " + p);
    }

    public void stopPlay () {
        this.commands [this.commands.length - 1].stop ();
        Log.v ("RemoteAudio/" + this.getClass ().getSimpleName (),
        "Exit from business process");
    }
}
