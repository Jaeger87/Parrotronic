package com.example.parrotronicandroid;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {

    private RecordButton recordButton = null;
    private PlayButton   playButton = null;

    private MediaRecorder recorder = null;
    private MediaPlayer player = null;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String fileName = null;

    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    private static final String LOG_TAG = "MainParrot";

    @ViewById(R.id.mouthValueText)
    TextView mouthValueTextView;

    @ViewById(R.id.mouthBar)
    SeekBar mouthBar;

    @ViewById(R.id.eyesSwitch)
    Switch eyes;

    @ViewById(R.id.micfab)
    FloatingActionButton micFab;

    @ViewById(R.id.playFab)
    FloatingActionButton playFab;


    @ViewById(R.id.waveform)
    PlayerVisualizerView waveform;

    @AfterViews
    void AfterViews(){
        eyes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                eyesSwitchPressed();
            }
        });

        mouthBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mouthValueTextView.setText(""+ progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/audiorecordtest.3gp";

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        recordButton = new RecordButton(this, micFab);

        playButton = new PlayButton(this, playFab);

    }


    @Override
    public void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }

        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }


    private void eyesSwitchPressed()
    {
        Toast.makeText(this, "premuto", Toast.LENGTH_LONG).show();
    }


    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.prepare();


            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {

                    playFab.setImageResource(R.drawable.ic_play);
                    // stop streaming vocal note
                    if (player != null) {
                        stopPlaying();
                    }
                    playButton.mStartPlaying = true;
                }
            });

            player.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        player.release();
        player = null;
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;

        waveform.updateVisualizer(fileToBytes(new File(fileName)));
    }

    class RecordButton{
        Context ctx;
        boolean mStartRecording = true;

        View.OnClickListener clicker = new View.OnClickListener() {
            public void onClick(View v) {
                onRecord(mStartRecording);
                if (mStartRecording) {
                    micFab.setImageResource(R.drawable.ic_stop);
                } else {
                    micFab.setImageResource(R.drawable.ic_mic);
                }
                mStartRecording = !mStartRecording;
            }
        };

        public RecordButton(Context ctx, FloatingActionButton button) {
            this.ctx = ctx;
            button.setOnClickListener(clicker);
        }
    }

    class PlayButton{
        boolean mStartPlaying = true;
        boolean mPausePlaying = false;
        Context ctx;


        View.OnClickListener clicker = new View.OnClickListener() {
            public void onClick(View v) {
                onPlay(mStartPlaying);
                if (mStartPlaying) {
                    playFab.setImageResource(R.drawable.ic_pause);
                } else {
                    playFab.setImageResource(R.drawable.ic_play);
                }
                mStartPlaying = !mStartPlaying;
            }
        };

        public PlayButton(Context ctx, FloatingActionButton button) {
            this.ctx = ctx;
            button.setOnClickListener(clicker);
        }
    }


    public static byte[] fileToBytes(File file) {
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

}
