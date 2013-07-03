package org.toilelibre.libe.remoteaudio.process.command;

import org.toilelibre.libe.remoteaudio.process.props.Properties;

public class LoginSshCommand implements Command {

    private StringBuffer prefix;

    public void execute () {

    }

    public Object getResult () {
        return this.prefix.append (Properties.getInstance ().getSshCommand ());
    }

    public void setParameters (Object [] objects) {
        if (objects [0] instanceof StringBuffer) {
            this.prefix = (StringBuffer) objects [0];
        }
    }

    public void stop () {
        // TODO Auto-generated method stub

    }

}
