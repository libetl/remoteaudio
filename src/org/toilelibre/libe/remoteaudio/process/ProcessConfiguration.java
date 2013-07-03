package org.toilelibre.libe.remoteaudio.process;

import java.util.LinkedList;
import java.util.List;

import org.toilelibre.libe.remoteaudio.process.listener.BufferActionListener;
import org.toilelibre.libe.remoteaudio.process.listener.EndOfProcessActionListener;
import org.toilelibre.libe.remoteaudio.process.listener.StreamActionListener;


public class ProcessConfiguration {

    private List<BufferActionListener>       bals;
    private List<StreamActionListener>       sals;
    private List<EndOfProcessActionListener> eopls;
    private Object                           resultObject;

    public List<BufferActionListener> getBals () {
        return bals;
    }
    
    public List<StreamActionListener> getSals () {
        return sals;
    }


    public List<EndOfProcessActionListener> getEopls () {
        return eopls;
    }


    public ProcessConfiguration () {
        this.eopls = new LinkedList<EndOfProcessActionListener> ();
        this.bals = new LinkedList<BufferActionListener> ();
        this.sals = new LinkedList<StreamActionListener> ();
    }

    public void attachBufferActionListener (BufferActionListener bal) {
        this.bals.add (bal);
    }

    public void attachEndOfProcessActionListener (
            EndOfProcessActionListener eopl) {
        this.eopls.add (eopl);
    }

    public void attachStreamActionListener (StreamActionListener sal) {
        this.sals.add (sal);
    }

    public Object getProcessResultObject () {
        return this.resultObject;
    }
}
