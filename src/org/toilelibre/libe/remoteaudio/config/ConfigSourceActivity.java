package org.toilelibre.libe.remoteaudio.config;

import org.toilelibre.libe.remoteaudio.R;
import org.toilelibre.libe.remoteaudio.process.props.Properties;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class ConfigSourceActivity extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        this.setContentView (R.layout.configsource);
        this.findViewById (R.id.backbutton).setOnClickListener (
                new OnClickListener () {

                    public void onClick (View v) {
                        ConfigSourceActivity.this.finish ();
                    }

                });
        this.findViewById (R.id.okbutton).setOnClickListener (
                new OnClickListener () {

                    public void onClick (View v) {
                        Properties p = Properties.getInstance ();
                        p.setHost ( ((EditText) ConfigSourceActivity.this
                                .findViewById (R.id.editHost)).getText ()
                                .toString ());
                        p.setDevFile ( ((EditText) ConfigSourceActivity.this
                                .findViewById (R.id.editSndFile)).getText ()
                                .toString ());
                        p.setSshUser ( ((EditText) ConfigSourceActivity.this
                                .findViewById (R.id.editSshUser)).getText ()
                                .toString ());
                        p.setSshPassword ( ((EditText) ConfigSourceActivity.this
                                .findViewById (R.id.editSshPassword))
                                .getText ().toString ());
                        p.setUrlPath ( ((EditText) ConfigSourceActivity.this
                                .findViewById (R.id.editUrlPath)).getText ()
                                .toString ());
                        Properties.getInstance ().persist ();
                        ConfigSourceActivity.this.finish ();
                    }

                });
        Properties p = Properties.getInstance ();
        ((EditText) ConfigSourceActivity.this.findViewById (R.id.editHost))
                .setText (p.getHost ());
        ((EditText) ConfigSourceActivity.this.findViewById (R.id.editSndFile))
                .setText (p.getDevFile ());
        ((EditText) ConfigSourceActivity.this.findViewById (R.id.editSshUser))
                .setText (p.getSshUser ());
        ((EditText) ConfigSourceActivity.this
                .findViewById (R.id.editSshPassword)).setText (p
                .getSshPassword ());
        ((EditText) ConfigSourceActivity.this.findViewById (R.id.editUrlPath))
                .setText (p.getUrlPath ());
    }
}
