package ch.ethz.ssh2;

import java.io.IOException;
import java.io.InputStream;

/**
 * A <code>StreamGobbler</code> is an InputStream that uses an internal worker
 * thread to constantly consume input from another InputStream. It uses a buffer
 * to store the consumed data. The buffer size is automatically adjusted, if
 * needed.
 * <p>
 * This class is sometimes very convenient - if you wrap a session's STDOUT and
 * STDERR InputStreams with instances of this class, then you don't have to
 * bother about the shared window of STDOUT and STDERR in the low level SSH-2
 * protocol, since all arriving data will be immediatelly consumed by the worker
 * threads. Also, as a side effect, the streams will be buffered (e.g., single
 * byte read() operations are faster).
 * <p>
 * Other SSH for Java libraries include this functionality by default in their
 * STDOUT and STDERR InputStream implementations, however, please be aware that
 * this approach has also a downside:
 * <p>
 * If you do not call the StreamGobbler's <code>read()</code> method often
 * enough and the peer is constantly sending huge amounts of data, then you will
 * sooner or later encounter a low memory situation due to the aggregated data
 * (well, it also depends on the Java heap size). Joe Average will like this
 * class anyway - a paranoid programmer would never use such an approach.
 * <p>
 * The term "StreamGobbler" was taken from an article called
 * "When Runtime.exec() won't", see
 * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: StreamGobbler.java,v 1.4 2006/02/14 19:43:16 cplattne Exp $
 */

public class StreamGobbler extends InputStream {
    class GobblerThread extends Thread {
        
        private static final int length = 65536;
        
        private boolean stop;

        public void setStop (boolean stop) {
            this.stop = stop;
        }

        @Override
        public void run () {
            byte [] buff = new byte [GobblerThread.length];

            while (!this.stop) {
                try {
                    int avail = StreamGobbler.this.is.read (buff, 0, buff.length);

                    synchronized (StreamGobbler.this.synchronizer) {
                        if (avail <= 0) {
                            StreamGobbler.this.isEOF = true;
                            StreamGobbler.this.synchronizer.notifyAll ();
                            break;
                        }

                        int space_available = StreamGobbler.this.buffer.length
                                - StreamGobbler.this.write_pos;

                        if (space_available < avail) {
                            /* compact/resize buffer */

                            int unread_size = StreamGobbler.this.write_pos
                                    - StreamGobbler.this.read_pos;
                            int need_space = unread_size + avail;

                            byte [] new_buffer = StreamGobbler.this.buffer;

                            if (need_space > StreamGobbler.this.buffer.length) {
                                int inc = need_space / 3;
                                inc = inc < 256 ? 256 : inc;
                                inc = inc > GobblerThread.length ? GobblerThread.length : inc;
                                new_buffer = new byte [need_space + inc];
                            }

                            if (unread_size > 0) {
                                System.arraycopy (StreamGobbler.this.buffer,
                                        StreamGobbler.this.read_pos,
                                        new_buffer, 0, unread_size);
                            }

                            StreamGobbler.this.buffer = new_buffer;

                            StreamGobbler.this.read_pos = 0;
                            StreamGobbler.this.write_pos = unread_size;
                        }

                        System.arraycopy (buff, 0, StreamGobbler.this.buffer,
                                StreamGobbler.this.write_pos, avail);
                        StreamGobbler.this.write_pos += avail;

                        StreamGobbler.this.synchronizer.notifyAll ();
                    }
                } catch (IOException e) {
                    synchronized (StreamGobbler.this.synchronizer) {
                        StreamGobbler.this.exception = e;
                        StreamGobbler.this.synchronizer.notifyAll ();
                        break;
                    }
                }
            }
        }
    }

    private InputStream   is;
    private GobblerThread t;

    private Object        synchronizer = new Object ();

    private boolean       isEOF        = false;
    private boolean       isClosed     = false;
    private IOException   exception    = null;

    private byte []       buffer       = new byte [65536];
    private int           read_pos     = 0;
    private int           write_pos    = 0;

    public StreamGobbler (InputStream is) {
        this.is = is;
        this.t = new GobblerThread ();
        this.t.setDaemon (true);
        this.t.start ();
    }

    @Override
    public int available () throws IOException {
        synchronized (this.synchronizer) {
            if (this.isClosed) {
                throw new IOException ("This StreamGobbler is closed.");
            }

            return this.write_pos - this.read_pos;
        }
    }

    @Override
    public void close () throws IOException {
        synchronized (this.synchronizer) {
            if (this.isClosed) {
                return;
            }
            this.isClosed = true;
            this.isEOF = true;
            this.synchronizer.notifyAll ();
            this.is.close ();
            this.t.setStop (true);
        }
    }

    @Override
    public int read () throws IOException {
        synchronized (this.synchronizer) {
            if (this.isClosed) {
                throw new IOException ("This StreamGobbler is closed.");
            }

            while (this.read_pos == this.write_pos) {
                if (this.exception != null) {
                    throw this.exception;
                }

                if (this.isEOF) {
                    return -1;
                }

                try {
                    this.synchronizer.wait ();
                } catch (InterruptedException e) {
                }
            }

            int b = this.buffer [this.read_pos++ ] & 0xff;

            return b;
        }
    }

    @Override
    public int read (byte [] b) throws IOException {
        return this.read (b, 0, b.length);
    }

    @Override
    public int read (byte [] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException ();
        }

        if (off < 0 || len < 0 || off + len > b.length || off + len < 0
                || off > b.length) {
            throw new IndexOutOfBoundsException ();
        }

        if (len == 0) {
            return 0;
        }

        synchronized (this.synchronizer) {
            if (this.isClosed) {
                throw new IOException ("This StreamGobbler is closed.");
            }

            while (this.read_pos == this.write_pos) {
                if (this.exception != null) {
                    throw this.exception;
                }

                if (this.isEOF) {
                    return -1;
                }

                try {
                    this.synchronizer.wait ();
                } catch (InterruptedException e) {
                }
            }

            int avail = this.write_pos - this.read_pos;

            avail = avail > len ? len : avail;

            System.arraycopy (this.buffer, this.read_pos, b, off, avail);

            this.read_pos += avail;

            return avail;
        }
    }
}
