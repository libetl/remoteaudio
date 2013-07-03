package org.toilelibre.libe.remoteaudio.process.props;

import org.toilelibre.libe.remoteaudio.process.ProcessConfiguration;

import android.content.Context;

public class SharedExecData {

    private static SharedExecData instance = new SharedExecData ();

    public static SharedExecData getInstance () {
        return SharedExecData.instance;
    }

    private ProcessConfiguration    processConfiguration;
    
    private Object                  resultLastExec;
    
    private Context                 context;
    

    public SharedExecData () {
        super ();
    }

    public Context getContext () {
        return this.context;
    }

    public void setContext (Context c) {
        this.context = c;
    }

    public ProcessConfiguration getProcessConfiguration () {
        return processConfiguration;
    }

    public void setProcessConfiguration (ProcessConfiguration processConfiguration) {
        this.processConfiguration = processConfiguration;
    }

    public Object getResultLastExec () {
        return resultLastExec;
    }

    public void setResultLastExec (Object resultLastExec) {
        this.resultLastExec = resultLastExec;
    }
    

    
}
