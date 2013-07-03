package ch.ethz.ssh2.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedList;

import ch.ethz.ssh2.log.Logger;

/**
 * TimeoutService (beta). Here you can register a timeout.
 * <p>
 * Implemented having large scale programs in mind: if you open many concurrent
 * SSH connections that rely on timeouts, then there will be only one timeout
 * thread. Once all timeouts have expired/are cancelled, the thread will (sooner
 * or later) exit. Only after new timeouts arrive a new thread (singleton) will
 * be instantiated.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: TimeoutService.java,v 1.2 2006/07/30 21:59:29 cplattne Exp $
 */
public class TimeoutService {
    private static class TimeoutThread extends Thread {
        @Override
        public void run () {
            synchronized (TimeoutService.todolist) {
                while (true) {
                    if (TimeoutService.todolist.size () == 0) {
                        TimeoutService.timeoutThread = null;
                        return;
                    }

                    long now = System.currentTimeMillis ();

                    TimeoutToken tt = TimeoutService.todolist.getFirst ();

                    if (tt.runTime > now) {
                        /* Not ready yet, sleep a little bit */

                        try {
                            TimeoutService.todolist.wait (tt.runTime - now);
                        } catch (InterruptedException e) {
                        }

                        /*
                         * We cannot simply go on, since it could be that the
                         * token was removed (cancelled) or another one has been
                         * inserted in the meantime.
                         */

                        continue;
                    }

                    TimeoutService.todolist.removeFirst ();

                    try {
                        tt.handler.run ();
                    } catch (Exception e) {
                        StringWriter sw = new StringWriter ();
                        e.printStackTrace (new PrintWriter (sw));
                        TimeoutService.log.log (
                                20,
                                "Exeception in Timeout handler:"
                                        + e.getMessage () + "("
                                        + sw.toString () + ")");
                    }
                }
            }
        }
    }

    public static class TimeoutToken implements Comparable<TimeoutToken> {
        private long     runTime;
        private Runnable handler;

        private TimeoutToken (long runTime, Runnable handler) {
            this.runTime = runTime;
            this.handler = handler;
        }

        public int compareTo (TimeoutToken t) {
            if (this.runTime > t.runTime) {
                return 1;
            }
            if (this.runTime == t.runTime) {
                return 0;
            }
            return -1;
        }
    }

    private static final Logger                   log           = Logger.getLogger (TimeoutService.class);

    /* The list object is also used for locking purposes */
    private static final LinkedList<TimeoutToken> todolist      = new LinkedList<TimeoutToken> ();

    private static Thread                         timeoutThread = null;

    /**
     * It is assumed that the passed handler will not execute for a long time.
     * 
     * @param runTime
     * @param handler
     * @return a TimeoutToken that can be used to cancel the timeout.
     */
    public static final TimeoutToken addTimeoutHandler (long runTime,
            Runnable handler) {
        TimeoutToken token = new TimeoutToken (runTime, handler);

        synchronized (TimeoutService.todolist) {
            TimeoutService.todolist.add (token);
            Collections.sort (TimeoutService.todolist);

            if (TimeoutService.timeoutThread != null) {
                TimeoutService.timeoutThread.interrupt ();
            } else {
                TimeoutService.timeoutThread = new TimeoutThread ();
                TimeoutService.timeoutThread.setDaemon (true);
                TimeoutService.timeoutThread.start ();
            }
        }

        return token;
    }

    public static final void cancelTimeoutHandler (TimeoutToken token) {
        synchronized (TimeoutService.todolist) {
            TimeoutService.todolist.remove (token);

            if (TimeoutService.timeoutThread != null) {
                TimeoutService.timeoutThread.interrupt ();
            }
        }
    }

}
