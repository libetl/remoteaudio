package org.toilelibre.libe.remoteaudio.process;


import org.toilelibre.libe.remoteaudio.process.props.SharedExecData;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ProcessBusinessService extends Service {

    private ProcessImpl                      pi;
    
    public ProcessBusinessService () {
    }
    
    @Override
    public synchronized void onStart (Intent intent, int startId) {
        ProcessConfiguration pc = SharedExecData.getInstance ().getProcessConfiguration ();
        this.pi = new ProcessImpl (pc);
        pi.run ();
    }
    
    @Override
    public void onDestroy () {
        pi.stopPlay ();
    }

    @Override
    public IBinder onBind (Intent intent) {
        return null;
    }
}
