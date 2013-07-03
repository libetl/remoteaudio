package org.toilelibre.libe.remoteaudio.config;

import java.util.List;

import org.toilelibre.libe.remoteaudio.R;
import org.toilelibre.libe.remoteaudio.process.props.Properties;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class ConfigCommandActivity extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        this.setContentView (R.layout.configcommand);
        List<String> commandNames = Properties.getInstance ()
                .getCommandsNames ();
        ((Spinner) this.findViewById (R.id.commandchoice))
                .setAdapter (new ArrayAdapter<String> (this,
                        android.R.layout.simple_spinner_item, commandNames));
        ((Spinner) this.findViewById (R.id.commandchoice))
                .setSelection (commandNames.indexOf (Properties.getInstance ()
                        .getActiveCommandsSet ()));
        this.findViewById (R.id.backbutton).setOnClickListener (
                new OnClickListener () {

                    public void onClick (View v) {
                        ConfigCommandActivity.this.finish ();
                    }

                });
        this.findViewById (R.id.okbutton).setOnClickListener (
                new OnClickListener () {

                    public void onClick (View v) {
                        Properties.getInstance ().setActiveCommandsSet (
                                (String) ((Spinner) ConfigCommandActivity.this
                                        .findViewById (R.id.commandchoice))
                                        .getSelectedItem ());
                        Properties.getInstance ().persist ();
                        ConfigCommandActivity.this.finish ();
                    }

                });
    }
}
