package com.example.parrotronicandroid;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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

    private static final String TAG = "MainParrot";

    private ArrayList<Byte> amplitudeList;

    private Timer _timer;

    private WaveFormUpdater waveFormUpdater;

    private Handler mWaveFormUpdateHandler;

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
            if(!playButton.mStartPlaying) {
                playButton.mStartPlaying = false;
                playFab.callOnClick();
            }
            stopPlaying();

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

    @Override
    public void onDestroy()
    {
        super.onDestroy();
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
            pausePlaying();
        }
    }

    private void startPlaying() {

        if(player == null) {
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


                mWaveFormUpdateHandler = new Handler();
                waveFormUpdater = new WaveFormUpdater(waveform, player, mWaveFormUpdateHandler);

                mWaveFormUpdateHandler.postDelayed(waveFormUpdater, 0);

            } catch (IOException e) {
                Log.e(TAG, "prepare() failed");
            }
        }

        player.start();
    }

    private void stopPlaying() {
        if(player != null) {
            mWaveFormUpdateHandler.removeCallbacks(waveFormUpdater);
            player.stop();
            player.release();
            player = null;
        }
    }

    private void pausePlaying() {
        player.pause();
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);

        recorder.setAudioSamplingRate(22050);
        recorder.setAudioEncodingBitRate(128000);
        recorder.setAudioChannels(2);

        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        amplitudeList = new ArrayList<>();

        _timer = new Timer();

        _timer.schedule(new TimerTask() {
            @Override
            public void run() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int amplitude = recorder.getMaxAmplitude();
                        Log.d(TAG, "" + amplitude);
                        Log.i(TAG, "" + map(amplitude, 0, 32762,0,255));
                        amplitudeList.add((byte)map(amplitude, 0, 32762,0,255));
                    }
                });
            }
        },200,100);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        recorder.start();
    }


    //Thank you processing
    static public final float map(float value,
                                  float istart,
                                  float istop,
                                  float ostart,
                                  float ostop) {
        return ostart + (ostop - ostart) * ((value - istart) / (istop - istart));
    }


    private void stopRecording() {
        _timer.cancel();
        recorder.stop();
        recorder.release();
        recorder = null;

        waveform.updateVisualizer(convertBytes(amplitudeList));

        Log.d(TAG, amplitudeList.toString());
    }


    public static byte[] convertBytes(List<Byte> bytes)
    {
        byte[] ret = new byte[bytes.size()];
        for (int i=0; i < ret.length; i++)
        {
            ret[i] = bytes.get(i).byteValue();
        }
        return ret;
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
