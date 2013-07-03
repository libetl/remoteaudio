package org.toilelibre.libe.remoteaudio.config;

import java.util.LinkedList;
import java.util.List;

import org.toilelibre.libe.remoteaudio.R;
import org.toilelibre.libe.remoteaudio.process.props.Properties;

import android.app.Activity;
import android.media.AudioFormat;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class ConfigStreamingActivity extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        this.setContentView (R.layout.configstreaming);

        Properties p = Properties.getInstance ();

        List<String> channelConfigs = new LinkedList<String> ();
        List<String> encodings = new LinkedList<String> ();
        List<String> samplerates = new LinkedList<String> ();
        List<String> sampleFormats = new LinkedList<String> ();

        channelConfigs.add ("Mono");
        channelConfigs.add ("Stereo");

        encodings.add ("PCM 8bit");
        encodings.add ("PCM 16bit");

        sampleFormats.add ("MP3");
        sampleFormats.add ("PCM");

        samplerates.add ("1000");
        samplerates.add ("2000");
        samplerates.add ("4000");
        samplerates.add ("8000");
        samplerates.add ("11025");
        samplerates.add ("22050");
        samplerates.add ("32768");
        samplerates.add ("44100");
        samplerates.add ("48000");
        samplerates.add ("96000");

        ((Spinner) this.findViewById (R.id.editChannelConfig))
                .setAdapter (new ArrayAdapter<String> (this,
                        android.R.layout.simple_spinner_item, channelConfigs));

        ((Spinner) this.findViewById (R.id.editEncoding))
                .setAdapter (new ArrayAdapter<String> (this,
                        android.R.layout.simple_spinner_item, encodings));

        ((Spinner) this.findViewById (R.id.editSamplerate))
                .setAdapter (new ArrayAdapter<String> (this,
                        android.R.layout.simple_spinner_item, samplerates));

        ((Spinner) this.findViewById (R.id.editSampleFormat))
                .setAdapter (new ArrayAdapter<String> (this,
                        android.R.layout.simple_spinner_item, sampleFormats));

        ((SeekBar) this.findViewById (R.id.barBufLength))
                .incrementProgressBy (8192);
        ((SeekBar) this.findViewById (R.id.barBufLength))
                .incrementSecondaryProgressBy (32768);
        ((SeekBar) this.findViewById (R.id.barBufLength)).setMax (524288);
        ((SeekBar) this.findViewById (R.id.barBufLength)).setProgress (p
                .getBufLength ());

        ((Spinner) this.findViewById (R.id.editChannelConfig))
                .setSelection (p.getChannelConfig () == AudioFormat.CHANNEL_CONFIGURATION_MONO ? 0
                        : 1);

        ((Spinner) this.findViewById (R.id.editEncoding)).setSelection (p
                .getEncoding () == AudioFormat.ENCODING_PCM_8BIT ? 0 : 1);

        ((Spinner) this.findViewById (R.id.editSamplerate))
                .setSelection (samplerates.indexOf ("" + p.getSampleRate ()));

        ((Spinner) this.findViewById (R.id.editSampleFormat))
                .setSelection (sampleFormats.indexOf ("" + p.getSampleFormat ()));

        ((TextView) this.findViewById (R.id.bufLengthValue)).setText (""
                + p.getBufLength ());

        ((SeekBar) this.findViewById (R.id.barBufLength))
                .setOnSeekBarChangeListener (new OnSeekBarChangeListener () {

                    public void onProgressChanged (SeekBar seekBar,
                            int progress, boolean fromUser) {
                        ((TextView) ConfigStreamingActivity.this
                                .findViewById (R.id.bufLengthValue))
                                .setText ("" + progress);
                    }

                    public void onStartTrackingTouch (SeekBar seekBar) {
                    }

                    public void onStopTrackingTouch (SeekBar seekBar) {
                        int progress = seekBar.getProgress ();
                        ((TextView) ConfigStreamingActivity.this
                                .findViewById (R.id.bufLengthValue))
                                .setText ("" + progress);
                    }

                });

        this.findViewById (R.id.backbutton).setOnClickListener (
                new OnClickListener () {

                    public void onClick (View v) {
                        ConfigStreamingActivity.this.finish ();
                    }

                });

        this.findViewById (R.id.okbutton).setOnClickListener (
                new OnClickListener () {

                    public void onClick (View v) {
                        Properties p = Properties.getInstance ();
                        p.setBufLength ( ((SeekBar) ConfigStreamingActivity.this
                                .findViewById (R.id.barBufLength))
                                .getProgress ());
                        p.setSampleRate (Integer
                                .parseInt ( ((Spinner) ConfigStreamingActivity.this
                                        .findViewById (R.id.editSamplerate))
                                        .getSelectedItem ().toString ()));
                        p.setSampleFormat ( ((Spinner) ConfigStreamingActivity.this
                                .findViewById (R.id.editSampleFormat))
                                .getSelectedItem ().toString ());

                        if ( ((Spinner) ConfigStreamingActivity.this
                                .findViewById (R.id.editChannelConfig))
                                .getSelectedItemId () == 0) {
                            p.setChannelConfig (AudioFormat.CHANNEL_CONFIGURATION_MONO);
                        } else {
                            p.setChannelConfig (AudioFormat.CHANNEL_CONFIGURATION_STEREO);
                        }

                        if ( ((Spinner) ConfigStreamingActivity.this
                                .findViewById (R.id.editEncoding))
                                .getSelectedItemId () == 0) {
                            p.setEncoding (AudioFormat.ENCODING_PCM_8BIT);
                        } else {
                            p.setEncoding (AudioFormat.ENCODING_PCM_16BIT);
                        }

                        Properties.getInstance ().persist ();
                        ConfigStreamingActivity.this.finish ();
                    }

                });
    }
}
