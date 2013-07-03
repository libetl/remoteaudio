package org.toilelibre.libe.remoteaudio.process.controler;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.toilelibre.libe.remoteaudio.process.listener.BufferActionListener;
import org.toilelibre.libe.remoteaudio.process.listener.EndOfProcessActionListener;
import org.toilelibre.libe.remoteaudio.process.listener.StreamActionListener;
import org.toilelibre.libe.remoteaudio.process.props.Properties;

public class ListenUnixCommandService extends Thread implements ListenService {

    private Process                          process;
    private List<BufferActionListener>       bals;
    private List<StreamActionListener>       sals;
    private List<EndOfProcessActionListener> eopls;

    private long                             totalSize = 0;

    private boolean                          stop      = false;
    private int                              result    = -1;
    private InputStream                      is;

    public ListenUnixCommandService (Process p, List<BufferActionListener> b,
            List<StreamActionListener> s, List<EndOfProcessActionListener> e) {
        this.process = p;
        this.bals = b;
        this.sals = s;
        this.eopls = e;
    }

    public int getResult () {
        return this.result;
    }

    @Override
    public void run () {
        this.setName (this.getClass ().getSimpleName ());
        this.is = this.process.getInputStream ();
        while ( !this.stop) {
            try {
                int size = 0;
                int max = Properties.getInstance ().getBufLength ();
                final AudioBuffer ab = new AudioBuffer (max);
                size = this.is.read (ab.getBuffer (), ab.getActualSize (), max);
                if (size <= 0) {
                    this.stop = true;
                    size = 0;
                }
                this.totalSize += size;
                ab.setActualSize (size);
                ab.setOffsetInBytes (this.totalSize);
                for (final BufferActionListener sal : this.bals) {
                    new Thread ("BufferActionListener-"
                            + sal.getClass ().getSimpleName ()) {
                        @Override
                        public void run () {
                            sal.onBytesRead (ab);
                        }
                    }.start ();
                }
            } catch (IOException e) {
                e.printStackTrace ();
            } catch (IllegalThreadStateException itse) {
                itse.printStackTrace ();
            }
        }
        try {
            this.process.getInputStream ().close ();
            this.process.getErrorStream ().close ();
            this.process.getOutputStream ().close ();
        } catch (IOException e) {
            e.printStackTrace ();
        }

        this.process.destroy ();
        try {
            this.result = this.process.exitValue ();
        } catch (IllegalThreadStateException itse) {
            this.result = 0;
        }
        for (EndOfProcessActionListener eopl : this.eopls) {
            eopl.onEnd (this.result);
        }
    }

    public void setStop (boolean stop) {
        this.stop = stop;
    }

    public void stream (AudioBuffer ab) {
        for (StreamActionListener sal : this.sals) {
            sal.onBytesStream (ab);
        }
    }

}
