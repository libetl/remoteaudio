package org.toilelibre.libe.remoteaudio.process.command;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.toilelibre.libe.remoteaudio.http.InitConnection;
import org.toilelibre.libe.remoteaudio.process.controler.ListenStreamService;
import org.toilelibre.libe.remoteaudio.process.listener.BufferActionListener;
import org.toilelibre.libe.remoteaudio.process.listener.EndOfProcessActionListener;
import org.toilelibre.libe.remoteaudio.process.listener.StreamActionListener;

public class MyWgetCommand implements Command {

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
        HttpClient httpclient = InitConnection.createClient ();
        HttpGet httpget = InitConnection
                .createHttpGet (host, address, protocol);
        HttpResponse hr = null;
        try {

            // Execute HTTP Get Request
            hr = httpclient.execute (httpget);

            if (hr.getStatusLine ().getStatusCode () != 200) {
                return;
            }

            HttpEntity he = hr.getEntity ();
            this.lt = new ListenStreamService (
                    InitConnection.getContentInputStream (he), this.sals,
                    this.bals, this.eopls);
            this.lt.start ();

        } catch (ClientProtocolException e) {
            e.printStackTrace ();
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
