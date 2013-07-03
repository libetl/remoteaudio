package org.toilelibre.libe.remoteaudio.process.command;

import org.toilelibre.libe.remoteaudio.process.props.Properties;

import android.media.AudioFormat;

public class SoxAlsaCommand implements Command {

    private StringBuffer prefix;

    public void execute () {

    }

    public Object getResult () {
        return this.prefix.append ("sox -" 
        		+ (Properties.getInstance().getEncoding() == AudioFormat.ENCODING_PCM_8BIT ? "1" : "2")
        		+ " -c " + (Properties.getInstance().getChannelConfig() == AudioFormat.CHANNEL_CONFIGURATION_MONO ? "1" : "2") + " -r "
                + Properties.getInstance ().getSampleRate ()
                + " -s -t alsa hw:0 -q -t raw - --single-threaded");
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
