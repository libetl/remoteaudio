package org.toilelibre.libe.remoteaudio.process.props;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.toilelibre.libe.remoteaudio.audio.format.Decoder;
import org.toilelibre.libe.remoteaudio.process.command.Command;
import org.toilelibre.libe.remoteaudio.process.command.GetCommand;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioFormat;

public class Properties {

    private static Properties instance = new Properties ();

    public static Properties getInstance () {
        return Properties.instance;
    }

    private String                  catCommand;

    private String                  padspCommand;

    private String                  sshCommand;

    private String                  wgetCommand;

    private String                  urlCommand;
    private String                  host              = "www.somehost.com";

    private String                  sshUser           = "ssh-user";

    private String                  sshPassword       = "password";

    private String                  devFile           = "/dev/audio";

    private String                  urlPath           = "/audio.php";

    private String                  activeCommandsSet = "[Java] open url then stream (Shoutcast compatible)";

    private String                  sampleFormat      = "PCM";

    private int                     bufLength         = 32768 * 4;
    
    private int                     delayMin          = bufLength;

    private int                     sampleRate        = 8000;

    private int                     channelConfig     = AudioFormat.CHANNEL_CONFIGURATION_MONO;

    private int                     encoding          = AudioFormat.ENCODING_PCM_8BIT;

    private Map<String, Command []> commands;

    private SharedPreferences       preferences;

    public Properties () {
        this.commands = new HashMap<String, Command []> ();

        this.commands.put (
                "[Java] Ssh with password then cat (be patient)",
                new Command [] { GetCommand.getCommand ("Empty"),
                        GetCommand.getCommand ("CatDevAudio"),
                        GetCommand.getCommand ("JavaSsh") });
        this.commands.put (
                "[Java] Ssh with password then padsp + cat (be patient)",
                new Command [] { GetCommand.getCommand ("Padsp"),
                        GetCommand.getCommand ("CatDevAudio"),
                        GetCommand.getCommand ("JavaSsh") });


        this.commands.put (
                "[Java] Ssh with password then ALSA + SoX",
                new Command [] { GetCommand.getCommand ("Empty"),
                        GetCommand.getCommand ("SoxAlsa"),
                        GetCommand.getCommand ("JavaSsh") });

        this.commands.put (
                "[Java] wget",
                new Command [] { GetCommand.getCommand ("Empty"),
                        GetCommand.getCommand ("Url"),
                        GetCommand.getCommand ("MyWget") });

        this.commands.put (
                "[Java] open url then stream (Shoutcast compatible)",
                new Command [] { GetCommand.getCommand ("Empty"),
                        GetCommand.getCommand ("Url"),
                        GetCommand.getCommand ("MyStream") });

        this.commands.put (
                "[Unix] Ssh without password then cat",
                new Command [] { GetCommand.getCommand ("LoginSsh"),
                        GetCommand.getCommand ("CatDevAudio"),
                        GetCommand.getCommand ("RunUnix") });

        this.commands.put (
                "[Unix] wget",
                new Command [] { GetCommand.getCommand ("Wget"),
                        GetCommand.getCommand ("Url"),
                        GetCommand.getCommand ("RunUnix") });

    }


    public String getActiveCommandsSet () {
        return this.activeCommandsSet;
    }

    public int getBufLength () {
        return this.bufLength;
    }

    public String getCatCommand () {
        return this.catCommand;
    }

    public int getChannelConfig () {
        return this.channelConfig;
    }

    public int getDelayMin () {
        return this.delayMin;
    }

    public void setDelayMin (int delayMin) {
        this.delayMin = delayMin;
    }

    public Command [] getCommandsByType (String type) {
        return this.commands.get (type);
    }

    public List<String> getCommandsNames () {
        final List<String> ls = new LinkedList<String> ();
        ls.addAll (this.commands.keySet ());
        return ls;
    }

    public Decoder getDecoder () {
        String package1 = Decoder.class.getPackage ().getName ();
        Class<?> clazz;
        try {
            clazz = Class.forName (package1 + "." + this.getSampleFormat ()
                    + "Decoder");
            return (Decoder) clazz.newInstance ();
        } catch (ClassNotFoundException e) {
            e.printStackTrace ();
        } catch (IllegalAccessException e) {
            e.printStackTrace ();
        } catch (InstantiationException e) {
            e.printStackTrace ();
        }
        return null;
    }

    public String getDevFile () {
        return this.devFile;
    }

    public int getEncoding () {
        return this.encoding;
    }

    public String getHost () {
        return this.host;
    }

    public String getSampleFormat () {
        return this.sampleFormat;
    }

    public int getSampleRate () {
        return this.sampleRate;
    }

    public String getSshCommand () {
        return this.sshCommand;
    }

    public String getSshPassword () {
        return this.sshPassword;
    }

    public String getSshUser () {
        return this.sshUser;
    }

    public String getUrlCommand () {
        return this.urlCommand;
    }

    public String getUrlPath () {
        return this.urlPath;
    }

    public String getWgetCommand () {
        return this.wgetCommand;
    }

	public String getPadspCommand() {
		return this.padspCommand;
	}

    private void implicitVars () {
        this.catCommand = "cat " + this.devFile + " ";
        this.padspCommand = "padsp ";
        this.sshCommand = "ssh " + this.sshUser + "@" + this.host + " ";

        this.wgetCommand = "wget -qO- ";

        this.urlCommand = "http://" + this.host + this.urlPath + " ";
    }

    public void init (SharedPreferences sp) {
        Class<Properties> c = Properties.class;
        Map<String, ?> map = sp.getAll ();
        this.preferences = sp;
        for (Field f : c.getDeclaredFields ()) {
            if (map.containsKey (f.getName ())) {
                try {
                    f.set (Properties.instance, map.get (f.getName ()));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace ();
                } catch (IllegalAccessException e) {
                    e.printStackTrace ();
                }
            }
        }
        this.implicitVars ();
    }

    public void persist () {
        if (this.preferences == null) {
            return;
        }
        Editor e = this.preferences.edit ();
        Class<Properties> c = Properties.class;
        for (Field f : c.getDeclaredFields ()) {
            if ( !f.getName ().endsWith ("Command")) {

                String type = f.getType ().getSimpleName ();
                type = type.substring (0, 1).toUpperCase ()
                        + type.substring (1);

                try {

                    Method m = e.getClass ().getMethod ("put" + type,
                            new Class [] { String.class, f.getType () });
                    m.invoke (e, new Object [] { f.getName (), f.get (this) });
                } catch (SecurityException e1) {
                    e1.printStackTrace ();
                } catch (NoSuchMethodException e1) {
                } catch (IllegalArgumentException e1) {
                    e1.printStackTrace ();
                } catch (IllegalAccessException e1) {
                    e1.printStackTrace ();
                } catch (InvocationTargetException e1) {
                    e1.printStackTrace ();
                }
            }
        }
        e.commit ();
        this.implicitVars ();
    }

    public void setActiveCommandsSet (String activeCommandsSet) {
        this.activeCommandsSet = activeCommandsSet;
    }

    public void setBufLength (int bufLength) {
        this.bufLength = bufLength;
    }

    public void setCatCommand (String catCommand) {
        this.catCommand = catCommand;
    }

    public void setChannelConfig (int channelConfig) {
        this.channelConfig = channelConfig;
    }

    public void setChannelConfig (String channelConfigurationStereo) {

    }

    public void setDevFile (String devFile) {
        this.devFile = devFile;
    }

    public void setEncoding (int encoding) {
        this.encoding = encoding;
    }

    public void setHost (String host) {
        this.host = host;
    }

    public void setSampleFormat (String sampleFormat) {
        this.sampleFormat = sampleFormat;
    }

    public void setSampleRate (int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void setSshCommand (String sshCommand) {
        this.sshCommand = sshCommand;
    }

    public void setSshPassword (String sshPassword) {
        this.sshPassword = sshPassword;
    }

    public void setSshUser (String sshUser) {
        this.sshUser = sshUser;
    }

    public void setUrlCommand (String urlCommand) {
        this.urlCommand = urlCommand;
    }

    public void setUrlPath (String urlPath) {
        this.urlPath = urlPath;
    }

    public void setWgetCommand (String wgetCommand) {
        this.wgetCommand = wgetCommand;
    }

}
