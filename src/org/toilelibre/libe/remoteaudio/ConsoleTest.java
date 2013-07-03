package org.toilelibre.libe.remoteaudio;

import org.toilelibre.libe.remoteaudio.process.ProcessConfiguration;
import org.toilelibre.libe.remoteaudio.process.ProcessImpl;
import org.toilelibre.libe.remoteaudio.process.debug.SysOutStreamActionListener;
import org.toilelibre.libe.remoteaudio.process.end.DefaultEndOfProcessActionListener;

public class ConsoleTest {

    public static void main (String [] args) {

        final ProcessConfiguration pc = new ProcessConfiguration ();
        pc.attachBufferActionListener (new SysOutStreamActionListener ());
        // pc.attachStreamActionListener (
        // new DriverStreamActionListener ());
        pc.attachEndOfProcessActionListener (new DefaultEndOfProcessActionListener ());
        new Thread (){public void run (){new ProcessImpl (pc).run ();}}.start();
    }
}
