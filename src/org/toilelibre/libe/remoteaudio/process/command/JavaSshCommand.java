package org.toilelibre.libe.remoteaudio.process.command;

import java.io.IOException;
import java.util.List;

import org.toilelibre.libe.remoteaudio.process.controler.ListenStreamService;
import org.toilelibre.libe.remoteaudio.process.listener.BufferActionListener;
import org.toilelibre.libe.remoteaudio.process.listener.EndOfProcessActionListener;
import org.toilelibre.libe.remoteaudio.process.listener.StreamActionListener;
import org.toilelibre.libe.remoteaudio.process.props.Properties;

import android.util.Log;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;

public class JavaSshCommand implements Command {

    private StringBuffer                     prefix = new StringBuffer ("");

    private List<BufferActionListener>       bals;

    private List<EndOfProcessActionListener> eopls;

    private List<StreamActionListener>       sals;

    private ListenStreamService               lt;
    
    private Session                          sshSession;
    
    private Connection                       sshConnection;

    public void execute () {

        Connection conn = new Connection (Properties.getInstance ().getHost ());
        try {
            conn.connect ();
            boolean isAuthenticated = conn.authenticateWithPassword (Properties
                    .getInstance ().getSshUser (), Properties.getInstance ()
                    .getSshPassword ());
            if (isAuthenticated == false) {
                throw new IOException ("Authentication failed.");
            }
            this.sshSession = conn.openSession ();
            Log.v ("RemoteAudio/" + this.getClass ().getSimpleName (),
                    this.prefix.toString ());
            this.sshSession.execCommand (this.prefix.toString ());
            this.lt = new ListenStreamService (this.sshSession.getStdout (), this.sals, this.bals,
                    this.eopls);
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
        if (this.sshSession != null){
            this.sshSession.close ();
        }
        if (this.sshConnection != null){
            this.sshConnection.close ();
        }
    }
}
