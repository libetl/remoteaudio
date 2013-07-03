package org.toilelibre.libe.remoteaudio.process.command;

import java.io.InputStream;
import java.net.SocketException;
import java.util.List;

import org.toilelibre.libe.remoteaudio.http.InitConnection;
import org.toilelibre.libe.remoteaudio.process.controler.ListenStreamService;
import org.toilelibre.libe.remoteaudio.process.listener.BufferActionListener;
import org.toilelibre.libe.remoteaudio.process.listener.EndOfProcessActionListener;
import org.toilelibre.libe.remoteaudio.process.listener.StreamActionListener;

public class MyStreamCommand implements Command {

    private StringBuffer                     prefix;
    private List<BufferActionListener>       bals;
    private List<EndOfProcessActionListener> eopls;
    private ListenStreamService               lt;
    private List<StreamActionListener>       sals;

    public void execute () {

        String [] url = this.prefix.toString ().split ("://");
        String protocol = url [0];
        String host = url [1].substring (0, url [1].indexOf ('/'));
        String address = url [1].substring (url [1].indexOf ('/')).trim ();

        try {
            InputStream is = InitConnection.getContentInputStream (protocol,
                    host, address);
            this.lt = new ListenStreamService (is, this.sals, this.bals,
                    this.eopls);
            this.lt.start ();
        } catch (SocketException se) {
            se.printStackTrace ();
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
