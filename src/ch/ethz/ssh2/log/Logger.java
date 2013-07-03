package ch.ethz.ssh2.log;

/**
 * Logger - a very simple logger, mainly used during development. Is not based
 * on log4j (to reduce external dependencies). However, if needed, something
 * like log4j could easily be hooked in.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: Logger.java,v 1.8 2006/10/06 12:55:40 cplattne Exp $
 */

public class Logger {
    private static boolean enabled  = false;
    private static int     logLevel = 99;

    public final static Logger getLogger (Class<?> x) {
        return new Logger (x);
    }

    public static void setEnabled (boolean e) {
        Logger.enabled = e;
    }

    public static void setLogLevel (int ll) {
        Logger.logLevel = ll;
    }

    private String className;

    public Logger (Class<?> x) {
        this.className = x.getName ();
    }

    public final boolean isEnabled () {
        return Logger.enabled;
    }

    public final void log (int level, String message) {
        if (Logger.enabled && level <= Logger.logLevel) {
            long now = System.currentTimeMillis ();

            synchronized (this) {
                System.err.println (now + " : " + this.className + ": "
                        + message);
                // or send it to log4j or whatever...
            }
        }
    }
}
