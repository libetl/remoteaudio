package org.toilelibre.libe.remoteaudio;

import org.toilelibre.libe.remoteaudio.config.ConfigCommandActivity;
import org.toilelibre.libe.remoteaudio.config.ConfigSourceActivity;
import org.toilelibre.libe.remoteaudio.config.ConfigStreamingActivity;
import org.toilelibre.libe.remoteaudio.process.ProcessBusinessService;
import org.toilelibre.libe.remoteaudio.process.ProcessConfiguration;
import org.toilelibre.libe.remoteaudio.process.debug.DisplaySizeOnReadBytesActionListener;
import org.toilelibre.libe.remoteaudio.process.driver.DriverStreamActionListener;
import org.toilelibre.libe.remoteaudio.process.end.DefaultEndOfProcessActionListener;
import org.toilelibre.libe.remoteaudio.process.gui.GUIEndOfProcessActionListener;
import org.toilelibre.libe.remoteaudio.process.gui.GUINotificationActionListener;
import org.toilelibre.libe.remoteaudio.process.gui.GUIReadBytesActionListener;
import org.toilelibre.libe.remoteaudio.process.gui.GUIStreamBytesActionListener;
import org.toilelibre.libe.remoteaudio.process.props.Properties;
import org.toilelibre.libe.remoteaudio.process.props.SharedExecData;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;

public class RemoteActivity extends Activity {

    private ProgressBar             bar;
    private boolean                 running;
    private ProcessConfiguration    pc;

    public void createNotification () {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) this
                .getSystemService (ns);
        int icon = R.drawable.notification_icon;

        CharSequence tickerText = "Remote Audio";
        long when = System.currentTimeMillis ();

        Notification notification = new Notification (icon, tickerText, when);
        notification.flags |= Notification.FLAG_AUTO_CANCEL
                | Notification.FLAG_ONGOING_EVENT;
        Context context = this.getApplicationContext ();
        CharSequence contentTitle = "Remote Audio";
        CharSequence contentText = "Streaming ("
                + Properties.getInstance ().getHost () + ")";
        Intent notificationIntent = this.getIntent ();
        PendingIntent contentIntent = PendingIntent.getActivity (this, 0,
                notificationIntent, 0);

        notification.setLatestEventInfo (context, contentTitle, contentText,
                contentIntent);

        mNotificationManager.notify (1, notification);

    }

    private void createProcess () {
        Handler h = new Handler ();
        this.bar = (ProgressBar) this.findViewById (R.id.stream);
        this.bar.setMax (100);
        this.bar.setProgress (0);
        
        this.pc = new ProcessConfiguration ();
        DriverStreamActionListener dsal = new DriverStreamActionListener ();
        this.pc.attachStreamActionListener (new DisplaySizeOnReadBytesActionListener ());
        this.pc.attachBufferActionListener (new GUIReadBytesActionListener (
                this, h));
        this.pc.attachBufferActionListener (dsal);
        this.pc.attachStreamActionListener (new GUIStreamBytesActionListener (
                this, h));
        this.pc.attachStreamActionListener (new GUINotificationActionListener (
                this, h));
        this.pc.attachEndOfProcessActionListener (new DefaultEndOfProcessActionListener (
                dsal));
        this.pc.attachEndOfProcessActionListener (new GUIEndOfProcessActionListener (
                this, h));
        this.pc.attachEndOfProcessActionListener (new GUINotificationActionListener (
                this, h));
    }

    public void dismissNotification () {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) this
                .getSystemService (ns);
        mNotificationManager.cancel (1);

    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        Properties.getInstance ().init (this.getSharedPreferences (this.getClass ()
                .getPackage ().getName (), Context.MODE_WORLD_WRITEABLE));
        this.setContentView (R.layout.main);
        this.running = false;
        this.createProcess ();
        this.findViewById (R.id.playpause).setOnClickListener (
                new OnClickListener () {

                    public void onClick (View v) {
                        if (RemoteActivity.this.running) {
                            RemoteActivity.this.stop ();
                        } else {
                            RemoteActivity.this.start ();
                        }
                        RemoteActivity.this.running = !RemoteActivity.this.running;
                    }

                });
        this.findViewById (R.id.configcommandbutton).setOnClickListener (
                new OnClickListener () {

                    public void onClick (View v) {
                        Intent i = new Intent (RemoteActivity.this,
                                ConfigCommandActivity.class);
                        RemoteActivity.this.startActivity (i);
                    }

                });

        this.findViewById (R.id.configstreambutton).setOnClickListener (
                new OnClickListener () {

                    public void onClick (View v) {
                        Intent i = new Intent (RemoteActivity.this,
                                ConfigSourceActivity.class);
                        RemoteActivity.this.startActivity (i);
                    }

                });

        this.findViewById (R.id.configplaybackbutton).setOnClickListener (
                new OnClickListener () {

                    public void onClick (View v) {
                        Intent i = new Intent (RemoteActivity.this,
                                ConfigStreamingActivity.class);
                        RemoteActivity.this.startActivity (i);
                    }

                });
    }

    public ProcessConfiguration getProcessConfiguration (){
        return this.pc;
    }
    
    public void setPBarReading (int current, int bufferLength) {
        if ( !this.bar.isIndeterminate ()) {
            this.bar.setProgress ((int) (current * 100.0 / bufferLength));
        }
    }

    public void setPBarStop () {
        this.bar.setIndeterminate (false);
        this.bar.setProgress (0);
        this.bar.invalidate ();
    }

    public void setPBarStreaming () {
        this.bar.setIndeterminate (true);
        this.bar.invalidate ();
    }

    public void start () {
        SharedExecData.getInstance ().setProcessConfiguration (this.pc);
        SharedExecData.getInstance ().setContext (this.getApplicationContext ());
        this.startService (new Intent (this, ProcessBusinessService.class));
    }

    public void stop () {
        this.stopService (new Intent (this, ProcessBusinessService.class));
        this.createProcess ();
    }
}
