package org.toilelibre.libe.remoteaudio.process.command;


public class GetCommand {

    public static Command getCommand (String name) {
        try {
            return (Command) Class.forName (
                    Command.class.getPackage ().getName () + "." + name
                            + Command.class.getSimpleName ()).newInstance ();
        } catch (InstantiationException e) {
            e.printStackTrace ();
        } catch (IllegalAccessException e) {
            e.printStackTrace ();
        } catch (ClassNotFoundException e) {
            e.printStackTrace ();
        }
        return null;
    }
}
